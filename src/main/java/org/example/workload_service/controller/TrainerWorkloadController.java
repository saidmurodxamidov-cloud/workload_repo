package org.example.workload_service.controller;

import lombok.RequiredArgsConstructor;
import org.example.workload_service.dto.TrainerWorkloadRequest;
import org.example.workload_service.dto.TrainerWorkloadResponse;
import org.example.workload_service.service.TrainerWorkloadService;
import org.example.workload_service.service.impl.TrainerWorkloadServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workloads")
@RequiredArgsConstructor
public class TrainerWorkloadController {

    private final TrainerWorkloadService service;

    @PostMapping
    public ResponseEntity<Void> updateWorkload(
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody TrainerWorkloadRequest request) {

        service.processWorkload(key,request);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{username}")
    public ResponseEntity<?> getSummary(@PathVariable String username){
         TrainerWorkloadResponse res = service.getSummary(username);
         if(res == null)
             return ResponseEntity.notFound().build();
         return ResponseEntity.ok().body(res);
    }
}
