package org.example.workload_service.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TrainerWorkloadResponse {

    private String username;
    private String firstName;
    private String lastName;
    private Boolean active;

    Map<Integer,Map<String,Integer>> yearsSummary = new HashMap<>();
}
