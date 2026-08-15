package com.example.overall.s9transactions.config;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan("com.example.overall.s9transactions")
// Enables the @Transactional proxy infrastructure.
@EnableTransactionManagement
public class AppConfig {

    // Real in-memory database for this lesson. No server setup needed; H2 lives only for this ApplicationContext/JVM.
    @Bean
    public DataSource dataSource() {
        return new SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:s9transactions;DB_CLOSE_DELAY=-1",
                "sa",
                "");
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    // @Transactional needs a transaction manager.
    // The proxy delegates begin/commit/rollback work to this PlatformTransactionManager.
    // JDBC apps commonly use DataSourceTransactionManager; JPA apps commonly use JpaTransactionManager.
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
