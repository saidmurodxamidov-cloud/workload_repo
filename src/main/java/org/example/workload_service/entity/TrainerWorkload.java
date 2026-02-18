package org.example.workload_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "trainer_workload")  // Explicit table name
@Getter
@Setter
public class TrainerWorkload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(unique = true)
    private String idempotencyKey;

    @Column(name = "username")
    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "is_active")
    private Boolean active;

    @Column(name = "work_year")
    private int year;

    @Column(name = "work_month")
    private int month;

    @Column(name = "total_duration")
    private int totalDuration;
}
