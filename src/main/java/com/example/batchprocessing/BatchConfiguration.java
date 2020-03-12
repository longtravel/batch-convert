package com.example.batchprocessing;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableBatchProcessing
@ComponentScan(basePackageClasses = {DatasourceConfiguration.class, MyBatchConfigurer.class})
public class BatchConfiguration {

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

  @Autowired
	public PlatformTransactionManager transactionManager;

  @Autowired
	private static Environment env;


  @Autowired
	public DataSource pgDataSource;

  @Autowired
	public DataSource legacyDataSource;

	@Bean
	public ColumnRangePartitioner partitioner()
	{
		ColumnRangePartitioner columnRangePartitioner = new ColumnRangePartitioner();
		columnRangePartitioner.setColumn("doc_id");
		columnRangePartitioner.setDataSource(pgDataSource);
		columnRangePartitioner.setTable("legacydocument");
		return columnRangePartitioner;
	}


	/*public ItemReader itemReader(@Qualifier("sourceDb") final DataSource dataSource) {
		JdbcCursorItemReader<SourceTable> reader = new JdbcCursorItemReader<>();*/

	@Bean
	public JdbcCursorItemReader<LegacyDocument> legacyItemReader(@Qualifier("legacyDataSource") final DataSource legacyDataSource) {
		return new JdbcCursorItemReaderBuilder<LegacyDocument>()
				.dataSource(legacyDataSource)
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
	public JdbcBatchItemWriter<LegacyDocument> writer(@Qualifier("pgDataSource") final DataSource dataSource) {
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
			.start(step1(writer(pgDataSource))).next(step2())
			.build();
	}

	@Bean
	public Step step1(JdbcBatchItemWriter<LegacyDocument> writer) {
		return stepBuilderFactory.get("step1")
			.<LegacyDocument, LegacyDocument> chunk(10)
			.reader(legacyItemReader(legacyDataSource))
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
		reader.setDataSource(pgDataSource);
		//TODO: put this in the application.props file via enviroment setting
		reader.setFetchSize(100);
		reader.setRowMapper(new DocumentRowMapper());
		reader.setQueryProvider(queryProvider);

		return reader;
	}


  /*
    - create rest http client (use properties)
    - try to make call so dave doc
    - read response. If good, set Document response to true. If bad, set to false
    - return document
  */
  @Bean
	@StepScope
  public DocumentItemProcessor docProcessor() {
		return new DocumentItemProcessor();
  }

  // Write into a NEW table (new object) in Postgres with just the results of the REST post
	// First, just write to a table. Then add the RESTful calls
	@Bean
	@StepScope
	public JdbcBatchItemWriter<Document> resultItemWriter()
	{
		JdbcBatchItemWriter<Document> itemWriter = new JdbcBatchItemWriter<>();
		itemWriter.setDataSource(pgDataSource);
		itemWriter.setSql("INSERT INTO result (newdoc, note) VALUES (:newdoc , :docLocator)");

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
				.gridSize(2)
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
        .processor(docProcessor())
				.writer(resultItemWriter())
				.build();
	}
}