package org.example.workload_service.respository;

import org.example.workload_service.entity.TrainerWorkloadDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainerWorkloadMongoRepository extends MongoRepository<TrainerWorkloadDocument, String> {
    Optional<TrainerWorkloadDocument> findByUsername(String username);
    boolean existsByUsername(String username);
}