package com.cryptotax.dto;

import com.cryptotax.model.JobStatus;

public record JobStatusDto(
        String jobId,
        JobStatus status,
        Object result,
        String errorMessage
) {}
