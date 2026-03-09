package org.example.workload_service.bdd.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.jms.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.example.workload_service.Enum.ActionType;
import org.example.workload_service.dto.TrainerWorkloadRequest;
import org.example.workload_service.entity.TrainerWorkload;
import org.example.workload_service.respository.TrainerWorkloadRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RequiredArgsConstructor
public class WorkloadConsumerSteps {

    private final JmsTemplate jmsTemplate;
    private final TrainerWorkloadRepository repository;

    @Value("${app.queue.workload}")
    private String workloadQueue;

    @When("a JMS message is sent to the workload queue with:")
    public void aJmsMessageIsSentToTheWorkloadQueueWith(DataTable dataTable) {
        sendMessage(buildRequest(dataTable), null, null, null);
    }

    @When("a JMS message is sent with idempotency key {string} and body:")
    public void aJmsMessageIsSentWithIdempotencyKeyAndBody(String idempotencyKey, DataTable dataTable) {
        sendMessage(buildRequest(dataTable), idempotencyKey, null, null);
    }

    @When("a JMS message is sent with traceId {string} spanId {string} and body:")
    public void aJmsMessageIsSentWithTraceIdSpanIdAndBody(
            String traceId, String spanId, DataTable dataTable) {
        sendMessage(buildRequest(dataTable), null, traceId, spanId);
    }

    @When("a JMS message is sent with no idempotency key and body:")
    public void aJmsMessageIsSentWithNoIdempotencyKeyAndBody(DataTable dataTable) {
        sendMessage(buildRequest(dataTable), null, null, null);
    }

    @Then("within {int} seconds a workload record exists for {string} in year {int} month {int} with duration {int}")
    public void withinSecondsAWorkloadRecordExistsWithDuration(
            int timeoutSeconds, String username, int year, int month, int expectedDuration) {

        Awaitility.await()
                .atMost(timeoutSeconds, TimeUnit.SECONDS)
                .pollInterval(200, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Optional<TrainerWorkload> opt =
                            repository.findByUsernameAndYearAndMonth(username, year, month);

                    assertThat(opt)
                            .as("No workload found for %s %d/%d", username, year, month)
                            .isPresent();

                    assertThat(opt.get().getTotalDuration())
                            .as("Duration mismatch for %s %d/%d", username, year, month)
                            .isEqualTo(expectedDuration);
                });
    }


    private void sendMessage(
            TrainerWorkloadRequest request,
            String idempotencyKey,
            String traceId,
            String spanId) {

        jmsTemplate.convertAndSend(workloadQueue, request, (Message message) -> {
            if (idempotencyKey != null) {
                message.setStringProperty("idempotencyKey", idempotencyKey);
            }
            if (traceId != null) {
                message.setStringProperty("traceId", traceId);
            }
            if (spanId != null) {
                message.setStringProperty("spanId", spanId);
            }
            log.debug("Sending JMS message to queue={} idempotencyKey={}", workloadQueue, idempotencyKey);
            return message;
        });
    }

    private TrainerWorkloadRequest buildRequest(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        Map<String, String> row = rows.get(0);

        TrainerWorkloadRequest request = new TrainerWorkloadRequest();
        request.setUsername(row.get("username"));
        request.setFirstName(row.get("firstName"));
        request.setLastName(row.get("lastName"));
        request.setActive(Boolean.parseBoolean(row.get("active")));
        request.setTrainingDate(LocalDate.parse(row.get("trainingDate")));
        request.setDuration(Integer.parseInt(row.get("duration")));
        request.setActionType(ActionType.valueOf(row.get("actionType")));
        return request;
    }
}