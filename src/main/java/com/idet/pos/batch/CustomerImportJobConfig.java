package com.idet.pos.batch;

import com.idet.pos.dto.CustomerDTO;
import com.idet.pos.entity.Customer;
import jakarta.persistence.EntityManagerFactory;
import jakarta.validation.Validator;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JpaItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.infrastructure.item.validator.ValidationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Chunk-oriented CSV import of customers (header: id,name,address,email).
 * Started only through POST /batch/customers/import; never on application startup.
 */
@Configuration
public class CustomerImportJobConfig {

    public static final String JOB_NAME = "customerImportJob";
    public static final String INPUT_FILE_PARAM = "input.file";
    static final int CHUNK_SIZE = 100;
    static final int SKIP_LIMIT = 100;

    @Bean(name = JOB_NAME)
    public Job customerImportJob(JobRepository jobRepository,
                                 @Qualifier("customerImportStep") Step customerImportStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(customerImportStep)
                .build();
    }

    @Bean
    public Step customerImportStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   @Qualifier("customerCsvReader") FlatFileItemReader<CustomerDTO> customerCsvReader,
                                   Validator validator,
                                   EntityManagerFactory entityManagerFactory) {
        return new StepBuilder("customerImportStep", jobRepository)
                .<CustomerDTO, Customer>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(customerCsvReader)
                .processor(new CustomerImportProcessor(validator))
                .writer(new JpaItemWriter<>(entityManagerFactory))
                .faultTolerant()
                .skip(FlatFileParseException.class, ValidationException.class)
                .skipLimit(SKIP_LIMIT)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<CustomerDTO> customerCsvReader(
            @Value("#{jobParameters['" + INPUT_FILE_PARAM + "']}") String inputFile) {
        return new FlatFileItemReaderBuilder<CustomerDTO>()
                .name("customerCsvReader")
                .resource(new FileSystemResource(inputFile))
                .linesToSkip(1)
                .delimited()
                .names("id", "name", "address", "email")
                .fieldSetMapper(fieldSet -> new CustomerDTO(
                        fieldSet.readString("id"),
                        fieldSet.readString("name"),
                        ImportValidation.blankToNull(fieldSet.readString("address")),
                        ImportValidation.blankToNull(fieldSet.readString("email"))))
                .build();
    }
}
