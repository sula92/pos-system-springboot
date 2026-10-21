package com.idet.pos.batch;

import com.idet.pos.entity.Customer;
import com.idet.pos.entity.Item;
import com.idet.pos.repository.CustomerRepository;
import com.idet.pos.repository.InventoryRepository;
import com.idet.pos.repository.ItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@SpringBatchTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:pos_batch_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
class ImportJobsTest {

    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    @Qualifier(CustomerImportJobConfig.JOB_NAME)
    private Job customerImportJob;

    @Autowired
    @Qualifier(ItemImportJobConfig.JOB_NAME)
    private Job itemImportJob;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void customerImportJob_writesValidRowsAndSkipsInvalidOnes() throws Exception {
        jobOperatorTestUtils.setJob(customerImportJob);

        JobExecution execution = jobOperatorTestUtils.startJob(parametersFor("batch/customers.csv"));

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        StepExecution step = execution.getStepExecutions().iterator().next();
        assertEquals(3, step.getReadCount());
        assertEquals(2, step.getWriteCount());
        assertEquals(1, step.getProcessSkipCount());

        Customer imported = customerRepository.findById("C902").orElseThrow();
        assertEquals("Batch Customer Two", imported.getName());
        assertEquals("two@example.com", imported.getEmail());
        assertTrue(customerRepository.existsById("C901"));
        assertFalse(customerRepository.existsById("BAD1"));
    }

    @Test
    void itemImportJob_createsNewItemsWithStockAndFiltersExistingOrRepeatedCodes() throws Exception {
        itemRepository.save(new Item("I905", "Already Exists", 5.0));
        jobOperatorTestUtils.setJob(itemImportJob);

        JobExecution execution = jobOperatorTestUtils.startJob(parametersFor("batch/items.csv"));

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        StepExecution step = execution.getStepExecutions().iterator().next();
        assertEquals(5, step.getReadCount());
        assertEquals(1, step.getReadSkipCount());
        assertEquals(1, step.getProcessSkipCount());
        assertEquals(2, step.getFilterCount());
        assertEquals(2, step.getWriteCount());

        assertEquals("Batch Rice 1kg", itemRepository.findById("I901").orElseThrow().getDescription());
        assertEquals(40, inventoryRepository.findById("I901").orElseThrow().getQty());
        assertEquals(0, inventoryRepository.findById("I902").orElseThrow().getQty());
        assertEquals(5.0, itemRepository.findById("I905").orElseThrow().getUnitPrice());
        assertFalse(itemRepository.existsById("I904"));
    }

    @Test
    void importEndpoint_runsJobAndReturnsSummary() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "customers.csv", "text/csv",
                "id,name,address,email\nC911,Endpoint Customer,Galle,endpoint@example.com\n".getBytes());

        mockMvc.perform(multipart("/batch/customers/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobName").value(CustomerImportJobConfig.JOB_NAME))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.readCount").value(1))
                .andExpect(jsonPath("$.writeCount").value(1))
                .andExpect(jsonPath("$.skipCount").value(0));

        assertTrue(customerRepository.existsById("C911"));
    }

    @Test
    void importEndpoint_rejectsEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/batch/items/import").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A non-empty CSV file is required"));
    }

    private JobParameters parametersFor(String classpathCsv) throws Exception {
        return new JobParametersBuilder(jobOperatorTestUtils.getUniqueJobParameters())
                .addString(CustomerImportJobConfig.INPUT_FILE_PARAM,
                        new ClassPathResource(classpathCsv).getFile().getAbsolutePath())
                .toJobParameters();
    }
}
