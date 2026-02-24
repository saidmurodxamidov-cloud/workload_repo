package org.example.workload_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.workload_service.Enum.ActionType;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@ToString
@NoArgsConstructor  // <--- Jackson needs this to create the object
@AllArgsConstructor
public class TrainerWorkloadRequest {

    private String username;
    private String firstName;
    private String lastName;
    private Boolean active;


    private LocalDate trainingDate;
    private int duration;

    private ActionType actionType;
}
