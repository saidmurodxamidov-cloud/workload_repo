package org.example.workload_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.workload_service.Enum.ActionType;
import org.example.workload_service.Enum.MonthEnum;
import org.example.workload_service.dto.TrainerWorkloadRequest;
import org.example.workload_service.dto.TrainerWorkloadResponse;
import org.example.workload_service.entity.TrainerWorkloadDocument;
import org.example.workload_service.respository.TrainerWorkloadMongoRepository;
import org.example.workload_service.service.TrainerWorkloadService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Profile("mongo")
@Slf4j
public class TrainerWorkloadMongoServiceImpl implements TrainerWorkloadService {


        private final TrainerWorkloadMongoRepository repository;

        public void processWorkload(String idempotencyKey, TrainerWorkloadRequest request) {
            TrainerWorkloadDocument workload = repository
                    .findByUsername(request.getUsername())
                    .orElseGet(() -> createNew(request));

            if (idempotencyKey != null) {
                if (workload.getIdempotencyKeys().contains(idempotencyKey)) {
                    log.info("Duplicate idempotency key: {}", idempotencyKey);
                    return;
                }
                workload.getIdempotencyKeys().add(idempotencyKey);
            }

            String year  = String.valueOf(request.getTrainingDate().getYear());
            String month = MonthEnum.fromInt(request.getTrainingDate().getMonthValue()).getDisplayName();

            workload.getYearsSummary().putIfAbsent(year, new HashMap<>());
            Map<String, Integer> monthMap = workload.getYearsSummary().get(year);
            int current = monthMap.getOrDefault(month, 0);

            if (request.getActionType() == ActionType.ADD) {
                monthMap.put(month, current + request.getDuration());
            } else {
                monthMap.put(month, Math.max(0, current - request.getDuration()));
            }

            repository.save(workload);
            log.info("Workload updated for: {}", request.getUsername());
        }

        private TrainerWorkloadDocument createNew(TrainerWorkloadRequest request) {
            TrainerWorkloadDocument workload = new TrainerWorkloadDocument();
            workload.setUsername(request.getUsername());
            workload.setFirstName(request.getFirstName());
            workload.setLastName(request.getLastName());
            workload.setActive(request.getActive());
            log.info("New trainer workload document created for: {}", request.getUsername());
            return workload;
        }

        public TrainerWorkloadResponse getSummary(String username) {
            return repository.findByUsername(username)
                    .map(workload -> {
                        TrainerWorkloadResponse response = new TrainerWorkloadResponse();
                        response.setUsername(workload.getUsername());
                        response.setFirstName(workload.getFirstName());
                        response.setLastName(workload.getLastName());
                        response.setActive(workload.getActive());
                        workload.getYearsSummary().forEach((year, months) ->
                                response.getYearsSummary()
                                        .put(Integer.parseInt(year), months)
                        );
                        return response;
                    })
                    .orElse(null);
    }
}
