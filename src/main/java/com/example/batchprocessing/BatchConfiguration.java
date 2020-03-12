package com.example.batchprocessing;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;

import org.springframework.batch.item.database.support.PostgresPagingQueryProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;


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

	@Bean
	public ColumnRangePartitioner partitioner()
	{
		ColumnRangePartitioner columnRangePartitioner = new ColumnRangePartitioner();
		columnRangePartitioner.setColumn("doc_id");
		columnRangePartitioner.setDataSource(dataSource);
		columnRangePartitioner.setTable("legacydocument");
		return columnRangePartitioner;
	}

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
				.sql("select DOC_HANDLE from CWSNS1.TSCNTRLT FETCH FIRST 500 ROWS ONLY") //for testing
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

	@Bean
	public Job importUserJob(JobCompletionNotificationListener listener, Step step1) {
		return jobBuilderFactory.get("importUserJob")
			.incrementer(new RunIdIncrementer())
			.listener(listener)
			.start(step1(writer(dataSource))).next(step2())
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

	@Bean
	@StepScope
	public JdbcPagingItemReader<Document> pagingItemReader(
			@Value("#{stepExecutionContext['minValue']}") Long minValue,
			@Value("#{stepExecutionContext['maxValue']}") Long maxValue)
	{
		System.out.println("reading " + minValue + " to " + maxValue);

		Map<String, Order> sortKeys = new HashMap<>();
		sortKeys.put("doc_id", Order.ASCENDING);

		PostgresPagingQueryProvider queryProvider = new PostgresPagingQueryProvider();
		queryProvider.setSelectClause("doc_id, doclocator");
		queryProvider.setFromClause("from legacydocument");
		queryProvider.setWhereClause("where doc_id >= " + minValue + " and doc_id < " + maxValue);
		queryProvider.setSortKeys(sortKeys);

		JdbcPagingItemReader<Document> reader = new JdbcPagingItemReader<>();
		reader.setDataSource(this.dataSource);
		//TODO: put this in the application.props file via enviroment setting
		reader.setFetchSize(100);
		reader.setRowMapper(new DocumentRowMapper());
		reader.setQueryProvider(queryProvider);

		return reader;
	}

  // Write into a NEW table (new object) in Postgres with just the results of the REST post
	// First, just write to a table. Then add the RESTful calls
	@Bean
	@StepScope
	public JdbcBatchItemWriter<Document> resultItemWriter()
	{
		JdbcBatchItemWriter<Document> itemWriter = new JdbcBatchItemWriter<>();
		itemWriter.setDataSource(dataSource);
		itemWriter.setSql("INSERT INTO result (newdoc, note) VALUES (1 , :docLocator)");

		itemWriter.setItemSqlParameterSourceProvider
				(new BeanPropertyItemSqlParameterSourceProvider<>());
		itemWriter.afterPropertiesSet();

		return itemWriter;
	}

	// Master
	@Bean
	public Step step2()
	{
		return stepBuilderFactory.get("step2")
				.partitioner(slaveStep().getName(), partitioner())
				.step(slaveStep())
				.gridSize(4)
				.taskExecutor(new SimpleAsyncTaskExecutor())
				.build();
	}

	// slave step
	@Bean
	public Step slaveStep()
	{
		return stepBuilderFactory.get("slaveStep")
				.<Document, Document>chunk(100)
				.reader(pagingItemReader(null, null))
				.writer(resultItemWriter())
				.build();
	}
}