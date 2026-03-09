package org.example.workload_service.bdd.runner;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(
        key   = GLUE_PROPERTY_NAME,
        value = "org.example.workload_service.bdd, org.example.workload_service.bdd.config"
)
@ConfigurationParameter(
        key   = PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-reports/workload-report.html, json:target/cucumber-reports/workload-report.json"
)
@ConfigurationParameter(
        key   = FILTER_TAGS_PROPERTY_NAME,
        value = "not @Ignore"
)
public class CucumberRunner {

}