package org.example.workload_service.service;

import org.example.workload_service.Enum.ActionType;
import org.example.workload_service.dto.TrainerWorkloadRequest;
import org.example.workload_service.dto.TrainerWorkloadResponse;
import org.example.workload_service.entity.TrainerWorkload;
import org.example.workload_service.respository.TrainerWorkloadRepository;
import org.example.workload_service.service.impl.TrainerWorkloadServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadServiceTest {

    @Mock
    private TrainerWorkloadRepository repository;

    @InjectMocks
    private TrainerWorkloadServiceImpl service;

    private static final String IDEMPOTENCY_KEY = "test-uuid-1234";

    private TrainerWorkloadRequest request;
    private TrainerWorkload existingWorkload;

    @BeforeEach
    void setUp() {
        request = new TrainerWorkloadRequest();
        request.setUsername("john.doe");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setActive(true);
        request.setTrainingDate(LocalDate.of(2024, 6, 15));
        request.setDuration(60);
        request.setActionType(ActionType.ADD);

        existingWorkload = new TrainerWorkload();
        existingWorkload.setUsername("john.doe");
        existingWorkload.setYear(2024);
        existingWorkload.setMonth(6);
        existingWorkload.setTotalDuration(120);
    }


    @Test
    void processWorkload_shouldSkip_whenDuplicateIdempotencyKey() {
        when(repository.existsByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(true);

        service.processWorkload(IDEMPOTENCY_KEY, request);

        verify(repository, never()).findByUsernameAndYearAndMonth(any(), anyInt(), anyInt());
        verify(repository, never()).save(any());
    }

    @Test
    void processWorkload_shouldProcess_whenIdempotencyKeyIsNew() {
        when(repository.existsByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(false);
        when(repository.findByUsernameAndYearAndMonth("john.doe", 2024, 6))
                .thenReturn(Optional.of(existingWorkload));

        service.processWorkload(IDEMPOTENCY_KEY, request);

        verify(repository).save(existingWorkload);
        assertThat(existingWorkload.getTotalDuration()).isEqualTo(180); // 120 + 60
    }

    @Test
    void processWorkload_shouldProcess_whenIdempotencyKeyIsNull() {
        // null key → skip duplicate check, process normally
        when(repository.findByUsernameAndYearAndMonth("john.doe", 2024, 6))
                .thenReturn(Optional.of(existingWorkload));

        service.processWorkload(null, request);

        verify(repository).save(existingWorkload);
    }

    @Test
    void processWorkload_shouldAddDuration_whenActionTypeIsAdd() {
        when(repository.existsByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(false);
        when(repository.findByUsernameAndYearAndMonth("john.doe", 2024, 6))
                .thenReturn(Optional.of(existingWorkload));

        service.processWorkload(IDEMPOTENCY_KEY, request);

        assertThat(existingWorkload.getTotalDuration()).isEqualTo(180); // 120 + 60
        verify(repository).save(existingWorkload);
    }

    @Test
    void processWorkload_shouldSubtractDuration_whenActionTypeIsDelete() {
        request.setActionType(ActionType.DELETE);
        when(repository.existsByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(false);
        when(repository.findByUsernameAndYearAndMonth("john.doe", 2024, 6))
                .thenReturn(Optional.of(existingWorkload));

        service.processWorkload(IDEMPOTENCY_KEY, request);

        assertThat(existingWorkload.getTotalDuration()).isEqualTo(60); // 120 - 60
        verify(repository).save(existingWorkload);
    }

    @Test
    void processWorkload_shouldNotGoBelowZero_whenDeleteExceedsTotal() {
        request.setActionType(ActionType.DELETE);
        request.setDuration(200); // more than existing 120
        when(repository.existsByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(false);
        when(repository.findByUsernameAndYearAndMonth("john.doe", 2024, 6))
                .thenReturn(Optional.of(existingWorkload));

        service.processWorkload(IDEMPOTENCY_KEY, request);

        assertThat(existingWorkload.getTotalDuration()).isEqualTo(120); // unchanged
        verify(repository).save(existingWorkload);
    }

    @Test
    void processWorkload_shouldCreateNewWorkload_whenNoneExists() {
        when(repository.existsByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(false);
        when(repository.findByUsernameAndYearAndMonth("john.doe", 2024, 6))
                .thenReturn(Optional.empty());

        service.processWorkload(IDEMPOTENCY_KEY, request);

        verify(repository).save(argThat(saved ->
                saved.getUsername().equals("john.doe") &&
                        saved.getYear() == 2024 &&
                        saved.getMonth() == 6 &&
                        saved.getTotalDuration() == 60 // 0 + 60
        ));
    }


    @Test
    void getSummary_shouldReturnNull_whenUsernameDoesNotExist() {
        when(repository.existsTrainerWorkloadByUsername("ghost")).thenReturn(false);
        when(repository.findByUsername("ghost")).thenReturn(List.of());

        TrainerWorkloadResponse response = service.getSummary("ghost");

        assertThat(response).isNull();
    }

    @Test
    void getSummary_shouldReturnResponse_withCorrectYearMonthMapping() {
        TrainerWorkload w1 = new TrainerWorkload();
        w1.setUsername("john.doe");
        w1.setFirstName("John");
        w1.setLastName("Doe");
        w1.setActive(true);
        w1.setYear(2024);
        w1.setMonth(6);
        w1.setTotalDuration(120);

        TrainerWorkload w2 = new TrainerWorkload();
        w2.setUsername("john.doe");
        w2.setFirstName("John");
        w2.setLastName("Doe");
        w2.setActive(true);
        w2.setYear(2024);
        w2.setMonth(7);
        w2.setTotalDuration(90);

        when(repository.existsTrainerWorkloadByUsername("john.doe")).thenReturn(true);
        when(repository.findByUsername("john.doe")).thenReturn(List.of(w1, w2));

        TrainerWorkloadResponse response = service.getSummary("john.doe");

        assertThat(response.getUsername()).isEqualTo("john.doe");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getActive()).isTrue();
        assertThat(response.getYearsSummary().get(2024)).containsEntry("Jun", 120);
        assertThat(response.getYearsSummary().get(2024)).containsEntry("Jul", 90);
    }

    @Test
    void getSummary_shouldGroupCorrectly_acrossMultipleYears() {
        TrainerWorkload w2023 = new TrainerWorkload();
        w2023.setUsername("john.doe"); w2023.setFirstName("John"); w2023.setLastName("Doe");
        w2023.setActive(true); w2023.setYear(2023); w2023.setMonth(12); w2023.setTotalDuration(200);

        TrainerWorkload w2024 = new TrainerWorkload();
        w2024.setUsername("john.doe"); w2024.setFirstName("John"); w2024.setLastName("Doe");
        w2024.setActive(true); w2024.setYear(2024); w2024.setMonth(1); w2024.setTotalDuration(150);

        when(repository.existsTrainerWorkloadByUsername("john.doe")).thenReturn(true);
        when(repository.findByUsername("john.doe")).thenReturn(List.of(w2023, w2024));

        TrainerWorkloadResponse response = service.getSummary("john.doe");

        assertThat(response.getYearsSummary()).containsKeys(2023, 2024);
        assertThat(response.getYearsSummary().get(2023)).containsEntry("Dec", 200);
        assertThat(response.getYearsSummary().get(2024)).containsEntry("Jan", 150);
    }
}