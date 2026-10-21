package com.idet.pos.controller;

import com.idet.pos.batch.CustomerImportJobConfig;
import com.idet.pos.batch.ItemImportJobConfig;
import com.idet.pos.exception.InvalidRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Starts the bulk CSV import jobs on demand. The jobs run synchronously and the response
 * summarises the run; HTTP 200 when the job completed, 422 otherwise.
 */
@RestController
@RequestMapping("/batch")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Batch", description = "Bulk CSV import jobs (Spring Batch)")
public class BatchJobController {

    private static final Logger logger = Logger.getLogger(BatchJobController.class.getName());

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    @Qualifier(CustomerImportJobConfig.JOB_NAME)
    private Job customerImportJob;

    @Autowired
    @Qualifier(ItemImportJobConfig.JOB_NAME)
    private Job itemImportJob;

    @PostMapping(value = "/customers/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import customers from CSV",
            description = "CSV header: id,name,address,email. Invalid rows are skipped; existing ids are updated.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import job completed"),
            @ApiResponse(responseCode = "400", description = "File missing or empty"),
            @ApiResponse(responseCode = "422", description = "Import job did not complete")
    })
    public ResponseEntity<Map<String, Object>> importCustomers(@RequestParam("file") MultipartFile file) throws Exception {
        return runImport(customerImportJob, file);
    }

    @PostMapping(value = "/items/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import items with opening stock from CSV",
            description = "CSV header: code,description,unitPrice,qty. Invalid rows are skipped; existing or repeated codes are filtered out.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Import job completed"),
            @ApiResponse(responseCode = "400", description = "File missing or empty"),
            @ApiResponse(responseCode = "422", description = "Import job did not complete")
    })
    public ResponseEntity<Map<String, Object>> importItems(@RequestParam("file") MultipartFile file) throws Exception {
        return runImport(itemImportJob, file);
    }

    private ResponseEntity<Map<String, Object>> runImport(Job job, MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("A non-empty CSV file is required");
        }

        Path inputFile = Files.createTempFile("pos-batch-", ".csv");
        try {
            file.transferTo(inputFile);
            JobParameters parameters = new JobParametersBuilder()
                    .addString(CustomerImportJobConfig.INPUT_FILE_PARAM, inputFile.toAbsolutePath().toString())
                    .addString("run.id", UUID.randomUUID().toString())
                    .toJobParameters();

            logger.info("Starting batch job " + job.getName());
            JobExecution execution = jobOperator.start(job, parameters);
            logger.info("Batch job " + job.getName() + " finished with status " + execution.getStatus());

            HttpStatus status = execution.getStatus() == BatchStatus.COMPLETED
                    ? HttpStatus.OK
                    : HttpStatus.UNPROCESSABLE_CONTENT;
            return ResponseEntity.status(status).body(summarize(job, execution));
        } finally {
            Files.deleteIfExists(inputFile);
        }
    }

    private Map<String, Object> summarize(Job job, JobExecution execution) {
        long readCount = 0;
        long writeCount = 0;
        long filterCount = 0;
        long skipCount = 0;
        for (StepExecution stepExecution : execution.getStepExecutions()) {
            readCount += stepExecution.getReadCount();
            writeCount += stepExecution.getWriteCount();
            filterCount += stepExecution.getFilterCount();
            skipCount += stepExecution.getSkipCount();
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("jobName", job.getName());
        summary.put("executionId", execution.getId());
        summary.put("status", execution.getStatus().name());
        summary.put("exitCode", execution.getExitStatus().getExitCode());
        summary.put("readCount", readCount);
        summary.put("writeCount", writeCount);
        summary.put("filterCount", filterCount);
        summary.put("skipCount", skipCount);
        return summary;
    }
}
