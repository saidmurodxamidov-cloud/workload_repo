package org.example.workload_service.bdd.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.example.workload_service.Enum.ActionType;
import org.example.workload_service.dto.TrainerWorkloadRequest;
import org.example.workload_service.entity.TrainerWorkload;
import org.example.workload_service.respository.TrainerWorkloadRepository;
import org.example.workload_service.service.TrainerWorkloadService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@RequiredArgsConstructor
public class WorkloadProcessingSteps {

    private final TrainerWorkloadService workloadService;
    private final TrainerWorkloadRepository repository;


    @When("a workload request is processed with:")
    public void aWorkloadRequestIsProcessedWith(DataTable dataTable) {
        TrainerWorkloadRequest request = buildRequest(dataTable);
        workloadService.processWorkload(null, request);
    }

    @When("a workload request is processed with idempotency key {string} and:")
    public void aWorkloadRequestIsProcessedWithKeyAnd(String key, DataTable dataTable) {
        TrainerWorkloadRequest request = buildRequest(dataTable);
        workloadService.processWorkload(key, request);
    }

    @And("the same request is processed again with idempotency key {string} and:")
    public void theSameRequestIsProcessedAgainWithKey(String key, DataTable dataTable) {
        TrainerWorkloadRequest request = buildRequest(dataTable);
        workloadService.processWorkload(key, request);
    }

    @And("a workload request is processed with no idempotency key and:")
    public void aWorkloadRequestIsProcessedWithNoKeyAnd(DataTable dataTable) {
        TrainerWorkloadRequest request = buildRequest(dataTable);
        workloadService.processWorkload(null, request);
    }

    @Then("a workload record exists for {string} in year {int} month {int} with duration {int}")
    public void aWorkloadRecordExistsForInYearMonthWithDuration(
            String username, int year, int month, int expectedDuration) {

        Optional<TrainerWorkload> opt = repository.findByUsernameAndYearAndMonth(username, year, month);

        assertThat(opt)
                .as("No workload record found for username=%s year=%d month=%d", username, year, month)
                .isPresent();

        assertThat(opt.get().getTotalDuration())
                .as("Duration mismatch for username=%s year=%d month=%d", username, year, month)
                .isEqualTo(expectedDuration);
    }


    private TrainerWorkloadRequest buildRequest(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        Map<String, String> row = rows.getFirst(); // always a single row in these scenarios

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