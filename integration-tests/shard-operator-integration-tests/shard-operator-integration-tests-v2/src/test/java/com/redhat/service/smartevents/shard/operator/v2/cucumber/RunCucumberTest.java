package com.redhat.service.smartevents.shard.operator.v2.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty,com.epam.reportportal.cucumber.ScenarioReporter")
// For transitive step packages
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.redhat.service.smartevents.shard.operator.v2.cucumber.steps,com.redhat.service.smartevents.integration.tests.steps")
public class RunCucumberTest {
}
