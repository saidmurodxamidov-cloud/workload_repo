package org.example.workload_service.respository;

import org.example.workload_service.entity.TrainerWorkload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainerWorkloadRepository extends JpaRepository<TrainerWorkload, Long> {

    Optional<TrainerWorkload> findByUsernameAndYearAndMonth(
            String username,
            int year,
            int month
    );
    List<TrainerWorkload> findByUsername(String username);

    Boolean existsTrainerWorkloadByUsername(String username);

    Boolean existsByIdempotencyKey(String idempotencyKey);
}
