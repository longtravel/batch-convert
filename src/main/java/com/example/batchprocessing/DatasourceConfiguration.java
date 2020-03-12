package com.example.batchprocessing;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration
public class DatasourceConfiguration {

  @Autowired
  private Environment env;

  @Bean
  @Primary
  public DataSource pgDataSource() {
    DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName("org.postgresql.Driver");
    dataSourceBuilder.url(env.getProperty("pgJdbc"));
    dataSourceBuilder.username(env.getProperty("pgUser"));
    dataSourceBuilder.password(env.getProperty("pgPass"));
    return dataSourceBuilder.build();
  }

  @Bean
  public DataSource legacyDataSource() {
    DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.driverClassName("com.ibm.db2.jcc.DB2Driver");
    //TODO: convert these to properties to read from an ENV file
    dataSourceBuilder.url(env.getProperty("db2Jdbc"));
    dataSourceBuilder.username(env.getProperty("db2User"));
    dataSourceBuilder.password(env.getProperty("db2Pass"));
    return dataSourceBuilder.build();
  }

}
