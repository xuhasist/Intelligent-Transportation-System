package com.demo.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.demo.repository.status",
        entityManagerFactoryRef = "statusEntityManagerFactory",
        transactionManagerRef = "statusTransactionManager"
)
public class StatusDataSourceConfig {
    @Bean(name = "statusDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.status")
    public DataSource statusDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "statusEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean statusEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("statusDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.demo.model.status")
                .persistenceUnit("status")
                .build();
    }

    @Bean(name = "statusTransactionManager")
    public PlatformTransactionManager statusTransactionManager(
            @Qualifier("statusEntityManagerFactory") EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "statusJdbcTemplate")
    public JdbcTemplate statusJdbcTemplate(@Qualifier("statusDataSource") DataSource statusDataSource) {
        return new JdbcTemplate(statusDataSource);
    }
}
