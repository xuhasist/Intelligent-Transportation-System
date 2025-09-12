package com.demo.config;

import com.demo.dto.ScenarioRuleDto;
import com.demo.exception.DynamicException;
import com.demo.model.dynamic.DynamicCsvFile;
import com.demo.util.JobCompletionNotificationListener;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.List;

@Configuration
public class BatchConfig {

    @Autowired
    @Qualifier("dynamicTransactionManager")
    private PlatformTransactionManager dynamicTransactionManager;

    @Autowired
    @Qualifier("dynamicDataSource")
    private DataSource dynamicDataSource;

    @Autowired
    @Qualifier("dynamicEntityManagerFactory")
    private EntityManagerFactory dynamicEntityManagerFactory;

    @Bean
    public DataSource dataSource(@Qualifier("dynamicDataSource") DataSource dynamicDataSource) {
        return dynamicDataSource;
    }

    @Bean
    public PlatformTransactionManager transactionManager(
            @Qualifier("dynamicTransactionManager") PlatformTransactionManager dynamicTransactionManager) {
        return dynamicTransactionManager;
    }

    // tell batch to use this JobRepository
    @Bean
    public JobRepository jobRepository() throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dynamicDataSource);
        factory.setTransactionManager(dynamicTransactionManager);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.afterPropertiesSet(); // Ensure required properties are set
        return jobLauncher;
    }

    @Bean
    @StepScope
    public FlatFileItemReader<ScenarioRuleDto> csvReader(
            // The key must be 'filePath' and the type is String
            @Value("#{jobParameters['filePath']}") String filePath) {

        // Use FileSystemResource to create a Resource object from the filePath string
        Resource resource = new FileSystemResource(filePath);

        // use the resource to set up the reader
        FlatFileItemReader<ScenarioRuleDto> reader = new FlatFileItemReader<>();
        reader.setResource(resource);
        reader.setLinesToSkip(1);

        DefaultLineMapper<ScenarioRuleDto> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setStrict(false);
        tokenizer.setNames("version", "action", "scenarioId", "ruleId",
                "type", "operator", "threshold", "intersectionId", "pathId",
                "phaseOrder", "subPhaseId", "statement");
        lineMapper.setLineTokenizer(tokenizer);

        BeanWrapperFieldSetMapper<ScenarioRuleDto> fieldSetMapper = new BeanWrapperFieldSetMapper<>();
        fieldSetMapper.setTargetType(ScenarioRuleDto.class);
        lineMapper.setFieldSetMapper(fieldSetMapper);

        reader.setLineMapper(lineMapper);
        return reader;
    }

    // writes entities to the database via JPA
    @Bean
    public JpaItemWriter<DynamicCsvFile> writer() {
        JpaItemWriter<DynamicCsvFile> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(dynamicEntityManagerFactory);
        return writer;
    }

    @Bean
    public ItemProcessor<ScenarioRuleDto, DynamicCsvFile> processor() {
        return dto -> {
            if (dto.getVersion() == null || dto.getVersion().isEmpty()) return null;

            int version = parseIntOrThrow(dto.getVersion(), "version");

            // check if action is keep, add, update, delete
            String action = dto.getAction();
            if (!List.of("K", "A", "U", "D").contains(action)) {
                throw new DynamicException("Action value error: " + action);
            }

            int scenarioId = parseIntOrThrow(dto.getScenarioId(), "scenarioId");
            int ruleId = parseIntOrThrow(dto.getRuleId(), "ruleId");

            String operator = dto.getOperator();
            if (!List.of(">", ">=", "=", "<=", "<", "!=").contains(operator)) {
                throw new DynamicException("Operator value error: " + operator);
            }

            return DynamicCsvFile.builder()
                    .version(version)
                    .action(action)
                    .scenarioId(scenarioId)
                    .ruleId(ruleId)
                    .type(dto.getType())
                    .operator(operator)
                    .threshold(dto.getThreshold())
                    .intersectionId(dto.getIntersectionId())
                    .pathId(dto.getPathId())
                    .phaseOrder(dto.getPhaseOrder())
                    .subPhaseId(dto.getSubPhaseId())
                    .statement(dto.getStatement())
                    .build();
        };
    }

    private int parseIntOrThrow(String value, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new DynamicException("Invalid " + fieldName + ": " + value);
        }
    }

    @Bean
    public Step insertStep(FlatFileItemReader<ScenarioRuleDto> insertReader) throws Exception {
        return new StepBuilder("insertStep", jobRepository())
                // <InputType, OutputType>
                .<ScenarioRuleDto, DynamicCsvFile>chunk(1000, dynamicTransactionManager)
                .reader(insertReader)
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean
    public Job importJob(
            JobCompletionNotificationListener listener,
            Step insertStep
    ) throws Exception {
        return new JobBuilder("import-job", jobRepository())
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(insertStep)  // Pass the Step bean directly
                .build();
    }
}
