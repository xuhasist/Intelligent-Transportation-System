package com.demo.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
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
        basePackages = "com.demo.repository.traffic",
        entityManagerFactoryRef = "trafficEntityManagerFactory",
        transactionManagerRef = "trafficTransactionManager"
)
public class TrafficDataSourceConfig {
    @Bean(name = "trafficDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.traffic")
    public DataSource trafficDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "trafficEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean trafficEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("trafficDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.demo.model.traffic")
                .persistenceUnit("traffic")
                .build();
    }

    @Bean(name = "trafficTransactionManager")
    public PlatformTransactionManager trafficTransactionManager(
            @Qualifier("trafficEntityManagerFactory") EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean(name = "trafficJdbcTemplate")
    public JdbcTemplate trafficJdbcTemplate(@Qualifier("trafficDataSource") DataSource trafficDataSource) {
        return new JdbcTemplate(trafficDataSource);
    }
}
