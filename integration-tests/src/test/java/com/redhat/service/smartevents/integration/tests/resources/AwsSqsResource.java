package com.redhat.service.smartevents.integration.tests.resources;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

import software.tnb.aws.sqs.service.SQS;
import software.tnb.common.service.ServiceFactory;

public class AwsSqsResource {

    private static final SQS SQS = ServiceFactory.create(SQS.class);

    // Manually triggering beforeAll and afterAll as these methods are intended to be triggered as JUnit5 Extension, however Cucumber support JUnit5 Extensions.

    @BeforeAll
    public static void beforeAll() throws Exception {
        SQS.beforeAll(null);
    }

    @AfterAll
    public static void afterAll() throws Exception {
        SQS.afterAll(null);
    }

    public static void postToSQSQueue(String message, String queueName) {
        SQS.validation().sendMessage(queueName, message);
    }

    public static void createQueue(String queueName) {
        if (!SQS.validation().queueExists(queueName)) {
            SQS.validation().createQueue(queueName);
        }
    }

    public static void deleteQueue(String queueName) {
        if (SQS.validation().queueExists(queueName)) {
            SQS.validation().deleteQueue(queueName);
        }
    }

}
