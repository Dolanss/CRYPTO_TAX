package com.cryptotax.model;

import java.util.concurrent.CompletableFuture;

public class CalculationJob {

    private final String jobId;
    private volatile JobStatus status;
    private volatile Object result;
    private volatile String errorMessage;
    private final CompletableFuture<Object> future;

    public CalculationJob(String jobId) {
        this.jobId = jobId;
        this.status = JobStatus.PENDING;
        this.future = new CompletableFuture<>();
    }

    public String getJobId()        { return jobId; }
    public JobStatus getStatus()    { return status; }
    public Object getResult()       { return result; }
    public String getErrorMessage() { return errorMessage; }

    public void markProcessing() {
        this.status = JobStatus.PROCESSING;
    }

    public void complete(Object result) {
        this.result = result;
        this.status = JobStatus.COMPLETED;
        this.future.complete(result);
    }

    public void fail(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = JobStatus.FAILED;
        this.future.completeExceptionally(new RuntimeException(errorMessage));
    }
}
