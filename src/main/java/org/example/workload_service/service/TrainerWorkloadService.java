package org.example.workload_service.service;

import org.example.workload_service.dto.TrainerWorkloadRequest;
import org.example.workload_service.dto.TrainerWorkloadResponse;

public interface TrainerWorkloadService {
    TrainerWorkloadResponse getSummary(String username);
    void processWorkload(String idempotencyKey, TrainerWorkloadRequest request);
}
