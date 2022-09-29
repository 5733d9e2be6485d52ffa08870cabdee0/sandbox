package com.redhat.service.smartevents.integration.tests.steps;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.redhat.service.smartevents.integration.tests.context.TestContext;

import io.cucumber.java.en.And;

public class ContextSteps {

    private static final String CONTEXT_FILE_PATH_ENV = "CONTEXT_FILE_PATH";

    private TestContext context;
    private ObjectMapper mapper = new ObjectMapper();

    public ContextSteps(TestContext context) {
        this.context = context;
        this.mapper.registerModule(new JavaTimeModule());
    }

    @And("Save test context")
    public void saveContext() throws IOException {
        File jsonFile = new File(contextFilePath());
        mapper.writeValue(jsonFile, this.context);
    }

    @And("Load bridges from saved context")
    public void loadBridges() throws IOException {
        File jsonFile = new File(contextFilePath());
        TestContext tmpContext = mapper.readValue(jsonFile, TestContext.class);
        this.context.setBridges(tmpContext.getAllBridges());
    }

    private String contextFilePath() {
        if (System.getenv(CONTEXT_FILE_PATH_ENV) != null) {
            return System.getenv(CONTEXT_FILE_PATH_ENV);
        } else {
            return "context.json";
        }
    }
}
