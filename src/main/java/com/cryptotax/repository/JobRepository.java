package com.cryptotax.repository;

import com.cryptotax.model.CalculationJob;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class JobRepository {

    private final ConcurrentHashMap<String, CalculationJob> store = new ConcurrentHashMap<>();

    public void save(CalculationJob job) {
        store.put(job.getJobId(), job);
    }

    public Optional<CalculationJob> findById(String jobId) {
        return Optional.ofNullable(store.get(jobId));
    }
}
