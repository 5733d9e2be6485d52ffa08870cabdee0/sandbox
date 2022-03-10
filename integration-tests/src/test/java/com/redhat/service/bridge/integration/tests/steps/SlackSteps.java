package com.redhat.service.bridge.integration.tests.steps;

import com.redhat.service.bridge.integration.tests.resources.SlackResource;

import io.cucumber.java.en.Then;
import io.restassured.response.Response;

import static com.redhat.service.bridge.integration.tests.common.SlackUtils.SLACK_CHANNEL;
import static com.redhat.service.bridge.integration.tests.common.SlackUtils.SLACK_URI;
import static com.redhat.service.bridge.integration.tests.common.SlackUtils.getSlackToken;
import static com.redhat.service.bridge.integration.tests.common.SlackUtils.slackMessage;
import static org.assertj.core.api.Assertions.assertThat;

public class SlackSteps {

    @Then("verify slack message is received in the slack app")
    public void slackActionTest() {
        Response response = SlackResource.getListOfSlackMessageResponse(SLACK_URI + SLACK_CHANNEL, getSlackToken());
        assertThat(response.getBody().jsonPath().getList("messages.text")).containsAnyOf(slackMessage);
    }
}
