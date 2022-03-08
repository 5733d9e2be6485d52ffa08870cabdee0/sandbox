
package com.redhat.service.bridge.integration.tests.steps;

import java.time.Duration;

import org.awaitility.Awaitility;

import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.bridge.integration.tests.resources.SlackResource;

import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

public class SlackSteps {

    private TestContext context;

    public SlackSteps(TestContext context) {
        this.context = context;
    }

    @Then("^Slack channel contains message with text \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void slackActionTest(String messageText, int timeoutMinutes) {
        String messageTextWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, messageText);
        System.out.println(messageTextWithoutPlaceholders);
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(1))
                .untilAsserted(
                        () -> assertThat(SlackResource.getListOfSlackMessages()).containsAnyOf(messageTextWithoutPlaceholders));
    }
}