package com.demo.config;

import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.demo.repository.its",
        entityManagerFactoryRef = "itsEntityManagerFactory",
        transactionManagerRef = "itsTransactionManager"
)
public class ItsDataSourceConfig {
    @Primary
    @Bean(name = "itsDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.its")  // link to application.properties
    public DataSource itsDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "itsEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean itsEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("itsDataSource") DataSource dataSource) {

        return builder
                .dataSource(dataSource)
                .packages("com.demo.model.its") // location of entity classes
                .persistenceUnit("its")
                .build();
    }

    @Primary
    @Bean(name = "itsTransactionManager")
    public PlatformTransactionManager itsTransactionManager(
            @Qualifier("itsEntityManagerFactory") EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }
}
