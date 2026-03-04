package org.example.workload_service.service;

import org.example.workload_service.Enum.ActionType;
import org.example.workload_service.Enum.MonthEnum;
import org.example.workload_service.dto.TrainerWorkloadRequest;
import org.example.workload_service.dto.TrainerWorkloadResponse;
import org.example.workload_service.entity.TrainerWorkloadDocument;
import org.example.workload_service.respository.TrainerWorkloadMongoRepository;
import org.example.workload_service.service.impl.TrainerWorkloadMongoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadMongoServiceImplTest {

    @Mock
    private TrainerWorkloadMongoRepository repository;

    @InjectMocks
    private TrainerWorkloadMongoServiceImpl service;

    private TrainerWorkloadRequest buildRequest(ActionType actionType, int duration) {
        TrainerWorkloadRequest request = new TrainerWorkloadRequest();
        request.setUsername("john.doe");
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setActive(true);
        request.setTrainingDate(LocalDate.of(2024, 3, 15));
        request.setDuration(duration);
        request.setActionType(actionType);
        return request;
    }


    private static final String MARCH = MonthEnum.fromInt(3).getDisplayName();
    private static final String JANUARY = MonthEnum.fromInt(1).getDisplayName();

    private TrainerWorkloadDocument buildExistingDocument() {
        TrainerWorkloadDocument doc = new TrainerWorkloadDocument();
        doc.setId("abc123");
        doc.setUsername("john.doe");
        doc.setFirstName("John");
        doc.setLastName("Doe");
        doc.setActive(true);

        Map<String, Integer> months = new HashMap<>();
        months.put(MARCH, 60);
        Map<String, Map<String, Integer>> yearsSummary = new HashMap<>();
        yearsSummary.put("2024", months);
        doc.setYearsSummary(yearsSummary);
        return doc;
    }

    private void stubSavePassthrough() {
        when(repository.save(any(TrainerWorkloadDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }


    @Nested
    @DisplayName("processWorkload — ADD action")
    class ProcessWorkloadAdd {

        @Test
        @DisplayName("Creates a new document when trainer does not exist yet")
        void createsNewDocumentForUnknownTrainer() {
            when(repository.findByUsername("john.doe")).thenReturn(Optional.empty());
            stubSavePassthrough();

            service.processWorkload(null, buildRequest(ActionType.ADD, 45));

            ArgumentCaptor<TrainerWorkloadDocument> captor =
                    ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
            verify(repository).save(captor.capture());
            TrainerWorkloadDocument saved = captor.getValue();

            assertThat(saved.getUsername()).isEqualTo("john.doe");
            assertThat(saved.getFirstName()).isEqualTo("John");
            assertThat(saved.getLastName()).isEqualTo("Doe");
            assertThat(saved.getActive()).isTrue();
            assertThat(saved.getYearsSummary().get("2024").get(MARCH)).isEqualTo(45);
        }

        @Test
        @DisplayName("Adds duration on top of existing value for the same month")
        void addsDurationToExistingMonthEntry() {
            when(repository.findByUsername("john.doe"))
                    .thenReturn(Optional.of(buildExistingDocument()));
            stubSavePassthrough();

            service.processWorkload(null, buildRequest(ActionType.ADD, 30));

            ArgumentCaptor<TrainerWorkloadDocument> captor =
                    ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getYearsSummary().get("2024").get(MARCH))
                    .isEqualTo(90); // 60 + 30
        }

        @Test
        @DisplayName("Initialises a new year entry when the year is first seen")
        void initialisesNewYearEntry() {
            TrainerWorkloadDocument doc = buildExistingDocument(); // has 2024/MARCH=60
            when(repository.findByUsername("john.doe")).thenReturn(Optional.of(doc));
            stubSavePassthrough();

            TrainerWorkloadRequest req = buildRequest(ActionType.ADD, 20);
            req.setTrainingDate(LocalDate.of(2025, 1, 10)); // new year
            service.processWorkload(null, req);

            ArgumentCaptor<TrainerWorkloadDocument> captor =
                    ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getYearsSummary()).containsKey("2025");
            assertThat(captor.getValue().getYearsSummary().get("2025").get(JANUARY))
                    .isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("processWorkload — DELETE action")
    class ProcessWorkloadDelete {

        @Test
        @DisplayName("Subtracts duration from an existing month entry")
        void subtractsDurationFromExistingEntry() {
            when(repository.findByUsername("john.doe"))
                    .thenReturn(Optional.of(buildExistingDocument()));
            stubSavePassthrough();

            service.processWorkload(null, buildRequest(ActionType.DELETE, 20));

            ArgumentCaptor<TrainerWorkloadDocument> captor =
                    ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getYearsSummary().get("2024").get(MARCH))
                    .isEqualTo(40); // 60 - 20
        }

        @Test
        @DisplayName("Clamps result to 0 when subtracting more than the stored value")
        void clampsDurationToZeroOnUnderflow() {
            when(repository.findByUsername("john.doe"))
                    .thenReturn(Optional.of(buildExistingDocument()));
            stubSavePassthrough();

            service.processWorkload(null, buildRequest(ActionType.DELETE, 200));

            ArgumentCaptor<TrainerWorkloadDocument> captor =
                    ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getYearsSummary().get("2024").get(MARCH))
                    .isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("processWorkload — idempotency")
    class ProcessWorkloadIdempotency {

        @Test
        @DisplayName("Skips processing and does NOT save when the same key is seen twice")
        void skipsOnDuplicateIdempotencyKey() {
            TrainerWorkloadDocument doc = buildExistingDocument();
            doc.getIdempotencyKeys().add("key-abc");
            when(repository.findByUsername("john.doe")).thenReturn(Optional.of(doc));

            service.processWorkload("key-abc", buildRequest(ActionType.ADD, 30));

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Stores the idempotency key and saves when the key is new")
        void storesNewIdempotencyKey() {
            when(repository.findByUsername("john.doe"))
                    .thenReturn(Optional.of(buildExistingDocument()));
            stubSavePassthrough();

            service.processWorkload("key-xyz", buildRequest(ActionType.ADD, 30));

            ArgumentCaptor<TrainerWorkloadDocument> captor =
                    ArgumentCaptor.forClass(TrainerWorkloadDocument.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getIdempotencyKeys()).contains("key-xyz");
        }

        @Test
        @DisplayName("Processes normally when idempotency key is null")
        void processesNormallyWhenKeyIsNull() {
            when(repository.findByUsername("john.doe"))
                    .thenReturn(Optional.of(buildExistingDocument()));
            stubSavePassthrough();

            service.processWorkload(null, buildRequest(ActionType.ADD, 10));

            verify(repository).save(any());
        }
    }

    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @Test
        @DisplayName("Returns a correctly mapped response for an existing trainer")
        void returnsMappedResponseForExistingTrainer() {
            when(repository.findByUsername("john.doe"))
                    .thenReturn(Optional.of(buildExistingDocument()));

            TrainerWorkloadResponse response = service.getSummary("john.doe");

            assertThat(response).isNotNull();
            assertThat(response.getUsername()).isEqualTo("john.doe");
            assertThat(response.getFirstName()).isEqualTo("John");
            assertThat(response.getLastName()).isEqualTo("Doe");
            assertThat(response.getActive()).isTrue();
            assertThat(response.getYearsSummary()).containsKey(2024);
            assertThat(response.getYearsSummary().get(2024).get(MARCH)).isEqualTo(60);
        }

        @Test
        @DisplayName("Returns null when the trainer is not found")
        void returnsNullWhenTrainerNotFound() {
            when(repository.findByUsername("unknown")).thenReturn(Optional.empty());

            TrainerWorkloadResponse response = service.getSummary("unknown");

            assertThat(response).isNull();
        }

        @Test
        @DisplayName("Maps all years and months correctly when multiple entries exist")
        void mapsMultipleYearsCorrectly() {
            TrainerWorkloadDocument doc = buildExistingDocument();
            Map<String, Integer> months2023 = new HashMap<>();
            months2023.put(JANUARY, 120);
            months2023.put(MonthEnum.fromInt(2).getDisplayName(), 90);
            doc.getYearsSummary().put("2023", months2023);
            when(repository.findByUsername("john.doe")).thenReturn(Optional.of(doc));

            TrainerWorkloadResponse response = service.getSummary("john.doe");

            assertThat(response.getYearsSummary()).containsKeys(2023, 2024);
            assertThat(response.getYearsSummary().get(2023).get(JANUARY)).isEqualTo(120);
            assertThat(response.getYearsSummary().get(2023).get(MonthEnum.fromInt(2).getDisplayName())).isEqualTo(90);
        }
    }
}