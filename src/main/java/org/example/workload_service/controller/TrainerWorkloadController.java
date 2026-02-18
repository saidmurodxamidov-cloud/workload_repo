package org.example.workload_service.controller;

import lombok.RequiredArgsConstructor;
import org.example.workload_service.dto.TrainerWorkloadRequest;
import org.example.workload_service.dto.TrainerWorkloadResponse;
import org.example.workload_service.service.TrainerWorkloadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workloads")
@RequiredArgsConstructor
public class TrainerWorkloadController {

    private final TrainerWorkloadService service;

    @PostMapping
    public ResponseEntity<Void> updateWorkload(
            @RequestBody TrainerWorkloadRequest request) {
        service.processWorkload(request);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/{username}")
    public ResponseEntity<?> getSummary(@PathVariable String username){
         TrainerWorkloadResponse res = service.getSummary(username);
         if(res == null)
             return ResponseEntity.notFound().build();
         return ResponseEntity.ok().body(res);
    }
    @DeleteMapping
    public  ResponseEntity<Void> deleteWorkload(@RequestBody TrainerWorkloadRequest request){
        service.processWorkload(request);
        return ResponseEntity.ok().build();
    }
}
