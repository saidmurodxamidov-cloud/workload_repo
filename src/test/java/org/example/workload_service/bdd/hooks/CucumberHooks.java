package org.example.workload_service.bdd.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.workload_service.respository.TrainerWorkloadRepository;

@Slf4j
@RequiredArgsConstructor
public class CucumberHooks {

    private final TrainerWorkloadRepository workloadRepository;

    @Before
    public void beforeScenario(Scenario scenario) {
        log.info("▶  START scenario: [{}]", scenario.getName());
        workloadRepository.deleteAll();
    }

    @After
    public void afterScenario(Scenario scenario) {
        if (scenario.isFailed()) {
            log.error("FAILED scenario: [{}]", scenario.getName());
        } else {
            log.info("PASSED scenario: [{}]", scenario.getName());
        }
        workloadRepository.deleteAll();
    }
}