package org.example.workload_service.entity;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Document(collection = "trainer_workloads")
@Data
public class TrainerWorkloadDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    @Indexed
    private String firstName;

    @Indexed
    private String lastName;
    private Boolean active;

    private Set<String> idempotencyKeys = new HashSet<>();

    private Map<String, Map<String, Integer>> yearsSummary = new HashMap<>();
}
