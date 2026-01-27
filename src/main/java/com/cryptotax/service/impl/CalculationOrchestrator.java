package com.cryptotax.service.impl;

import com.cryptotax.async.AsyncCalculationTask;
import com.cryptotax.dto.JobStatusDto;
import com.cryptotax.dto.TaxReportDto;
import com.cryptotax.exception.JobNotFoundException;
import com.cryptotax.model.CalculationJob;
import com.cryptotax.model.Transaction;
import com.cryptotax.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class CalculationOrchestrator {

    private final CsvParserService csvParser;
    private final TaxReportService taxReportService;
    private final AsyncCalculationTask asyncTask;
    private final JobRepository jobRepository;

    public CalculationOrchestrator(CsvParserService csvParser,
                                   TaxReportService taxReportService,
                                   AsyncCalculationTask asyncTask,
                                   JobRepository jobRepository) {
        this.csvParser = csvParser;
        this.taxReportService = taxReportService;
        this.asyncTask = asyncTask;
        this.jobRepository = jobRepository;
    }

    /** Synchronous calculation — suitable for small files. */
    public TaxReportDto calculateSync(MultipartFile file) {
        List<Transaction> transactions = csvParser.parse(file);
        return taxReportService.buildReport(transactions);
    }

    /** Kicks off async processing and returns a jobId immediately. */
    public String submitAsync(MultipartFile file) {
        List<Transaction> transactions = csvParser.parse(file);
        String jobId = UUID.randomUUID().toString();
        CalculationJob job = new CalculationJob(jobId);
        jobRepository.save(job);
        asyncTask.execute(job, transactions);
        return jobId;
    }

    public JobStatusDto getJobStatus(String jobId) {
        CalculationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
        return new JobStatusDto(job.getJobId(), job.getStatus(),
                job.getResult(), job.getErrorMessage());
    }
}
