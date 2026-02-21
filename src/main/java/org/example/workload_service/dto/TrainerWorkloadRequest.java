package org.example.workload_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.ToString;
import org.example.workload_service.Enum.ActionType;

import java.time.LocalDate;

@Data
@ToString
public class TrainerWorkloadRequest {

    private String username;
    private String firstName;
    private String lastName;
    private Boolean active;

    @NotNull(message = "trainingDate is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate trainingDate;
    private int duration;

    private ActionType actionType;
}
