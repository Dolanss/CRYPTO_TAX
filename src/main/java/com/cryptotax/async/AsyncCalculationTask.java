package com.cryptotax.async;

import com.cryptotax.model.CalculationJob;
import com.cryptotax.model.Transaction;
import com.cryptotax.service.impl.TaxReportService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
public class AsyncCalculationTask {

    private final TaxReportService taxReportService;

    public AsyncCalculationTask(TaxReportService taxReportService) {
        this.taxReportService = taxReportService;
    }

    @Async("calculationExecutor")
    public CompletableFuture<Void> execute(CalculationJob job, List<Transaction> transactions) {
        job.markProcessing();
        try {
            var report = taxReportService.buildReport(transactions);
            job.complete(report);
        } catch (Exception e) {
            job.fail(e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
}
