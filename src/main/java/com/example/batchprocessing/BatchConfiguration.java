package com.example.batchprocessing;

import java.sql.SQLException;
import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionManager;

// tag::setup[]
@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	public DataSource dataSource;


  @Autowired
	public PlatformTransactionManager transactionManager;
	// end::setup[]

	@Bean
	@Primary
	public DataSource dataSource() {
		DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
		dataSourceBuilder.driverClassName("org.postgresql.Driver");
		//TODO: convert these to properties to read from an ENV file
		dataSourceBuilder.url("jdbc:postgresql://postgresql.sbox-dcl.cwds.io:5432/dcl");
		dataSourceBuilder.username("rthompson");
		dataSourceBuilder.password("dGDY3RQer8Aj");
		return dataSourceBuilder.build();
	}

	@Bean
	public DataSource legacyDataSource() {
		DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
		dataSourceBuilder.driverClassName("com.ibm.db2.jcc.DB2Driver");
	//TODO: convert these to properties to read from an ENV file
		dataSourceBuilder.url("jdbc:db2://dblb-1.nonprod-gateway.cwds.io:4016/DBN1SOC:currentSchema=CWSNS1;");
		dataSourceBuilder.username("CWDSRTH");
		dataSourceBuilder.password("44Pull44");
		return dataSourceBuilder.build();
	}

	@Bean
	public JdbcCursorItemReader<LegacyDocument> legacyItemReader() {
		return new JdbcCursorItemReaderBuilder<LegacyDocument>()
				.dataSource(legacyDataSource())
				.name("legacyDocumentItemReader")
				.sql("select DOC_HANDLE from CWSNS1.TSCNTRLT FETCH FIRST 100 ROWS ONLY") //for testing
				//.sql("select DOC_HANDLE from CWSNS1.TSCNTRLT")
				.rowMapper(new LegacyDocumentRowMapper())
				.build();

	}

	@Bean
	public LegacyDocumentItemProcessor processor() {
		return new LegacyDocumentItemProcessor();
	}

	@Bean
	public JdbcBatchItemWriter<LegacyDocument> writer(DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<LegacyDocument>()
			.itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
			.sql("INSERT INTO legacyDocument (docLocator) VALUES (:docLocator)")
			.dataSource(dataSource)
			.build();
	}
	// end::readerwriterprocessor[]

	// tag::jobstep[]
	@Bean
	public Job importUserJob(JobCompletionNotificationListener listener, Step step1) {
		return jobBuilderFactory.get("importUserJob")
			.incrementer(new RunIdIncrementer())
			.listener(listener)
			.flow(step1)
			.end()
			.build();
	}

	//return jobs.get("myJob").start(step1()).next(step2()).build();

	@Bean
	public Step step1(JdbcBatchItemWriter<LegacyDocument> writer) {
		return stepBuilderFactory.get("step1")
			.<LegacyDocument, LegacyDocument> chunk(10)
			.reader(legacyItemReader())
			.processor(processor())
			.writer(writer)
			.build();
	}
	// end::jobstep[]
}



	/*
	@Bean
	protected JobRepository createJobRepository() throws Exception {
		JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		factory.setDataSource(dataSource);
		factory.setTransactionManager(transactionManager);
		factory.setIsolationLevelForCreate("ISOLATION_SERIALIZABLE");
		factory.setDatabaseType("H2");
		factory.setTablePrefix("BATCH_");
		factory.setMaxVarCharLength(1000);
		return factory.getObject();
	}
*/
// This would reside in your BatchConfigurer implementation
/*	@Bean
	protected JobRepository createJobRepository() throws Exception {
		MapJobRepositoryFactoryBean factory = new MapJobRepositoryFactoryBean();
		factory.setTransactionManager(transactionManager);
		return factory.getObject();
	}*/