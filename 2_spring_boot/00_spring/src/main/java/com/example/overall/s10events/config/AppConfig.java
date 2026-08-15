package com.example.overall.s10events.config;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableAsync
@EnableTransactionManagement
@ComponentScan("com.example.overall.s10events")
public class AppConfig {

    @Bean
    public DataSource dataSource() {
        return new SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:s10events;DB_CLOSE_DELAY=-1",
                "sa",
                "");
    }

    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
