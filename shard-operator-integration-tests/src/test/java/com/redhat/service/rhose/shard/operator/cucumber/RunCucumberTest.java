package com.redhat.service.rhose.shard.operator.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
// For transitive step packages
//@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.redhat.service.bridge.shard.operator.cucumber")
public class RunCucumberTest {
}