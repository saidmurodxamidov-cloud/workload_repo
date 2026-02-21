package org.example.workload_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.workload_service.Enum.ActionType;
import org.example.workload_service.dto.TrainerWorkloadRequest;
import org.example.workload_service.dto.TrainerWorkloadResponse;
import org.example.workload_service.service.TrainerWorkloadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TrainerWorkloadControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private TrainerWorkloadService service;

    @InjectMocks
    private TrainerWorkloadController controller;

    private static final String BASE_URL = "/api/workloads";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String TEST_IDEMPOTENCY_KEY = "test-uuid-1234";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Nested
    @DisplayName("POST /api/workloads (updateWorkload)")
    class UpdateWorkloadTests {

        @Test
        @DisplayName("should return 204 and call service with key and body when request is valid")
        void shouldReturn204AndCallService() throws Exception {
            TrainerWorkloadRequest request = new TrainerWorkloadRequest();
            request.setUsername("trainer1");
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setActive(true);
            request.setTrainingDate(LocalDate.of(2025, 2, 15));
            request.setDuration(120);
            request.setActionType(ActionType.ADD);

            mockMvc.perform(post(BASE_URL)
                            .header(IDEMPOTENCY_KEY_HEADER, TEST_IDEMPOTENCY_KEY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNoContent());

            // key comes from header, request from body — both forwarded to service
            verify(service).processWorkload(eq(TEST_IDEMPOTENCY_KEY), any(TrainerWorkloadRequest.class));
        }

        @Test
        @DisplayName("should return 400 when Idempotency-Key header is missing")
        void shouldReturn400WhenIdempotencyKeyMissing() throws Exception {
            TrainerWorkloadRequest request = new TrainerWorkloadRequest();
            request.setUsername("trainer1");
            request.setActionType(ActionType.ADD);
            request.setTrainingDate(LocalDate.of(2025, 2, 15));
            request.setDuration(60);

            // No Idempotency-Key header — Spring should reject with 400
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/workloads/{username} (getSummary)")
    class GetSummaryTests {

        @Test
        @DisplayName("should return 200 with body when summary exists")
        void shouldReturn200WithBodyWhenSummaryExists() throws Exception {
            String username = "trainer1";
            TrainerWorkloadResponse response = new TrainerWorkloadResponse();
            response.setUsername(username);
            response.setFirstName("John");
            response.setLastName("Doe");
            response.setActive(true);
            Map<Integer, Map<String, Integer>> yearsSummary = new HashMap<>();
            yearsSummary.put(2025, Map.of("Feb", 100));
            response.setYearsSummary(yearsSummary);

            when(service.getSummary(username)).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/" + username))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.username").value(username))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.yearsSummary.2025.Feb").value(100));

            verify(service).getSummary(username);
        }

        @Test
        @DisplayName("should return 404 when summary does not exist")
        void shouldReturn404WhenSummaryDoesNotExist() throws Exception {
            String username = "unknown";
            when(service.getSummary(username)).thenReturn(null);

            mockMvc.perform(get(BASE_URL + "/" + username))
                    .andExpect(status().isNotFound());

            verify(service).getSummary(username);
        }
    }
}