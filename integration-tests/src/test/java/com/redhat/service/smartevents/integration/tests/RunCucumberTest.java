package com.redhat.service.smartevents.integration.tests;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import io.cucumber.core.options.Constants;

import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/action-webhook.feature")
@ConfigurationParameter(key = Constants.FILTER_TAGS_PROPERTY_NAME, value = "not @ansibleaction")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "com.redhat.service.smartevents.integration.tests.cucumber.TestCaseFileLogger,com.epam.reportportal.cucumber.ScenarioReporter")
public class RunCucumberTest {
}
