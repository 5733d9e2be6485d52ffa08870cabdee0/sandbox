
package com.redhat.service.smartevents.integration.tests.steps;

import java.time.Duration;

import org.awaitility.Awaitility;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.integration.tests.resources.SlackResource;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

import static org.assertj.core.api.Assertions.assertThat;

public class SlackSteps {

    private TestContext context;

    public SlackSteps(TestContext context) {
        this.context = context;
    }

    @Then("^Slack channel \"([^\"]*)\" contains message with text \"([^\"]*)\" within (\\d+) (?:minute|minutes)$")
    public void slackActionTest(String channelName, String messageText, int timeoutMinutes) {
        String messageTextWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, messageText);
        String channelNameWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, channelName);
        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(
                        () -> assertThat(SlackResource.getListOfSlackMessages(channelNameWithoutPlaceholders))
                                .as("Searching for message on channel '%s' containing text: '%s'", channelNameWithoutPlaceholders, messageTextWithoutPlaceholders)
                                .anyMatch(msg -> msg.contains(messageTextWithoutPlaceholders)));
    }

    @And("^create message with text \"([^\"]*)\" on slack channel \"([^\"]*)\"$")
    public void createMessageOnSlackChannel(String messageText, String channelName) {
        String messageTextWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, messageText);
        String channelNameWithoutPlaceholders = ContextResolver.resolveWithScenarioContext(context, channelName);
        SlackResource.postToSlackWebhookUrl(messageTextWithoutPlaceholders, channelNameWithoutPlaceholders);
    }
}
