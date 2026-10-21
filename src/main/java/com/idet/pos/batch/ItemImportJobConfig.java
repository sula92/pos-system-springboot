package com.idet.pos.batch;

import com.idet.pos.entity.Item;
import com.idet.pos.repository.ItemRepository;
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
 * Chunk-oriented CSV import of new items with their opening stock (header: code,description,unitPrice,qty).
 * Started only through POST /batch/items/import; never on application startup.
 */
@Configuration
public class ItemImportJobConfig {

    public static final String JOB_NAME = "itemImportJob";

    @Bean(name = JOB_NAME)
    public Job itemImportJob(JobRepository jobRepository,
                             @Qualifier("itemImportStep") Step itemImportStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(itemImportStep)
                .build();
    }

    @Bean
    public Step itemImportStep(JobRepository jobRepository,
                               PlatformTransactionManager transactionManager,
                               @Qualifier("itemCsvReader") FlatFileItemReader<ItemCsvRow> itemCsvReader,
                               @Qualifier("itemImportProcessor") ItemImportProcessor itemImportProcessor,
                               EntityManagerFactory entityManagerFactory) {
        // New items are persisted, never merged: Inventory shares the item's assigned id (@MapsId),
        // and merging a new row of that shape fails on Hibernate 7.
        JpaItemWriter<Item> writer = new JpaItemWriter<>(entityManagerFactory);
        writer.setUsePersist(true);

        return new StepBuilder("itemImportStep", jobRepository)
                .<ItemCsvRow, Item>chunk(CustomerImportJobConfig.CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(itemCsvReader)
                .processor(itemImportProcessor)
                .writer(writer)
                .faultTolerant()
                .skip(FlatFileParseException.class, ValidationException.class)
                .skipLimit(CustomerImportJobConfig.SKIP_LIMIT)
                .build();
    }

    @Bean
    @StepScope
    public FlatFileItemReader<ItemCsvRow> itemCsvReader(
            @Value("#{jobParameters['" + CustomerImportJobConfig.INPUT_FILE_PARAM + "']}") String inputFile) {
        return new FlatFileItemReaderBuilder<ItemCsvRow>()
                .name("itemCsvReader")
                .resource(new FileSystemResource(inputFile))
                .linesToSkip(1)
                .delimited()
                .names("code", "description", "unitPrice", "qty")
                .fieldSetMapper(fieldSet -> new ItemCsvRow(
                        fieldSet.readString("code"),
                        fieldSet.readString("description"),
                        fieldSet.readDouble("unitPrice"),
                        fieldSet.readInt("qty")))
                .build();
    }

    @Bean
    @StepScope
    public ItemImportProcessor itemImportProcessor(ItemRepository itemRepository, Validator validator) {
        return new ItemImportProcessor(itemRepository, validator);
    }
}
