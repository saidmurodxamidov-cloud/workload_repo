package org.example.workload_service.bdd.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import lombok.RequiredArgsConstructor;
import org.example.workload_service.bdd.config.ScenarioContext;
import org.example.workload_service.entity.TrainerWorkload;
import org.example.workload_service.respository.TrainerWorkloadRepository;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class WorkloadSummarySteps {

    private final TrainerWorkloadRepository repository;
    private final ScenarioContext context;

    @LocalServerPort
    private int port;

    private static final String BASE_URL = "http://localhost";
    private static final String RESPONSE_KEY = "response";


    @Given("the workload database is clean")
    public void theWorkloadDatabaseIsClean() {
        repository.deleteAll();
    }


    @Given("a trainer workload exists for username {string} with the following data:")
    public void aTrainerWorkloadExistsForUsername(String username, DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps();
        rows.forEach(row -> {
            TrainerWorkload w = new TrainerWorkload();
            w.setUsername(username);
            w.setFirstName(row.get("firstName"));
            w.setLastName(row.get("lastName"));
            w.setActive(Boolean.parseBoolean(row.get("active")));
            w.setYear(Integer.parseInt(row.get("year")));
            w.setMonth(Integer.parseInt(row.get("month")));
            w.setTotalDuration(Integer.parseInt(row.get("totalDuration")));
            repository.save(w);
        });
    }

    @Given("no workload exists for username {string}")
    public void noWorkloadExistsForUsername(String username) {
        repository.deleteAll(repository.findByUsername(username));
    }

    // WHEN steps

    @When("I request the workload summary for trainer {string}")
    public void iRequestTheWorkloadSummaryForTrainer(String username) {
        Response response = RestAssured
                .given()
                .baseUri(BASE_URL)
                .port(port)
                .contentType("application/json")
                .when()
                .get("/api/workloads/{username}", username);

        context.set(RESPONSE_KEY, response);
    }



    @Then("the response status should be {int}")
    public void theResponseStatusShouldBe(int expectedStatus) {
        Response response = context.get(RESPONSE_KEY);
        assertThat(response.getStatusCode())
                .as("Expected HTTP status %d but got %d. Body: %s",
                        expectedStatus, response.getStatusCode(), response.getBody().asString())
                .isEqualTo(expectedStatus);
    }

    @And("the response username should be {string}")
    public void theResponseUsernameShouldBe(String expected) {
        Response response = context.get(RESPONSE_KEY);
        assertThat(response.jsonPath().getString("username")).isEqualTo(expected);
    }

    @And("the response firstName should be {string}")
    public void theResponseFirstNameShouldBe(String expected) {
        Response response = context.get(RESPONSE_KEY);
        assertThat(response.jsonPath().getString("firstName")).isEqualTo(expected);
    }

    @And("the response lastName should be {string}")
    public void theResponseLastNameShouldBe(String expected) {
        Response response = context.get(RESPONSE_KEY);
        assertThat(response.jsonPath().getString("lastName")).isEqualTo(expected);
    }

    @And("the trainer should be active")
    public void theTrainerShouldBeActive() {
        Response response = context.get(RESPONSE_KEY);
        assertThat(response.jsonPath().getBoolean("active")).isTrue();
    }

    @And("the trainer should be inactive")
    public void theTrainerShouldBeInactive() {
        Response response = context.get(RESPONSE_KEY);
        assertThat(response.jsonPath().getBoolean("active")).isFalse();
    }


    @And("the summary for year {int} month {string} should be {int}")
    public void theSummaryForYearMonthShouldBe(int year, String month, int expectedDuration) {
        Response response = context.get(RESPONSE_KEY);
//        System.out.println("DEBUG RESPONSE: " + response.asPrettyString());
        Integer actual = response.jsonPath().getInt("yearsSummary." + year + "." + month.substring(0,3));
        assertThat(actual)
                .as("Duration for %d/%s", year, month)
                .isEqualTo(expectedDuration);
    }
}