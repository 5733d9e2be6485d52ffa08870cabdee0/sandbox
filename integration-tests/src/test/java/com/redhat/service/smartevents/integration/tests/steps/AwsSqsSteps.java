package com.redhat.service.smartevents.integration.tests.steps;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.integration.tests.resources.AwsSqsResource;

import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

public class AwsSqsSteps {

    private TestContext context;

    public AwsSqsSteps(TestContext context) {
        this.context = context;
    }

    @And("^create an SQS queue on AWS called \"([^\"]*)\"$")
    public void createQueue(String queueName) {
        String resolvedQueueName = context.getSqsQueue(queueName);
        AwsSqsResource.createQueue(resolvedQueueName);
    }

    @When("^send a message with text \"([^\"]*)\" to \"([^\"]*)\" sqs queue$")
    public void sendMessage(String messageText, String queueName) {
        String messageTextWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, messageText);
        String resolvedQueueName = context.getSqsQueue(queueName);
        AwsSqsResource.postToSQSQueue(messageTextWithoutPlaceholders, resolvedQueueName);
    }
}
