package org.example.workload_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.workload_service.Enum.ActionType;
import org.example.workload_service.dto.TrainerWorkloadRequest;
import org.example.workload_service.dto.TrainerWorkloadResponse;
import org.example.workload_service.entity.TrainerWorkload;
import org.example.workload_service.respository.TrainerWorkloadRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainerWorkloadService {

    private final TrainerWorkloadRepository repository;

    public void processWorkload(TrainerWorkloadRequest request) {

        if(request.getIdempotencyKey() != null &&
            repository.existsByIdempotencyKey(request.getIdempotencyKey())){
            log.info("duplicate key is identified key: {}", request.getIdempotencyKey());
            return;
        }
        int year = request.getTrainingDate().getYear();
        int month = request.getTrainingDate().getMonthValue();

        TrainerWorkload workload = repository
                .findByUsernameAndYearAndMonth(
                        request.getUsername(),
                        year,
                        month
                )
                .orElseGet(() -> createNew(request, year, month));

        if (request.getActionType() == ActionType.ADD) {
            workload.setTotalDuration(workload.getTotalDuration() + request.getDuration());
        } else {
            if(workload.getTotalDuration() >= request.getDuration())
                 workload.setTotalDuration(workload.getTotalDuration() - request.getDuration());
        }

        repository.save(workload);
    }

    private TrainerWorkload createNew(
            TrainerWorkloadRequest request,
            int year,
            int month
    ) {
        TrainerWorkload workload = new TrainerWorkload();

        workload.setUsername(request.getUsername());
        workload.setFirstName(request.getFirstName());
        workload.setLastName(request.getLastName());
        workload.setActive(request.getActive());
        workload.setYear(year);
        workload.setMonth(month);
        workload.setTotalDuration(0);


        log.info("workload created successfully: {} on date: {}",request.getUsername(),request.getTrainingDate());
        return workload;
    }
    public TrainerWorkloadResponse getSummary(String username){
        String[] months = new String[]{"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        List<TrainerWorkload> workloads = repository.findByUsername(username);
        TrainerWorkloadResponse response = new TrainerWorkloadResponse();
        if(!repository.existsTrainerWorkloadByUsername(username))
            return null;
        response.setFirstName(workloads.getFirst().getFirstName());
        response.setLastName(workloads.getFirst().getLastName());
        response.setActive(workloads.getFirst().getActive());
        response.setUsername(username);

        Map<Integer, Map<String,Integer>> map = response.getYearsSummary();
        workloads.forEach(trainerWorkload -> {
            map.putIfAbsent(trainerWorkload.getYear(),new HashMap<>());
            map.get(trainerWorkload.getYear()).put(months[trainerWorkload.getMonth() - 1],trainerWorkload.getTotalDuration());
        });

        return response;
    }

}
