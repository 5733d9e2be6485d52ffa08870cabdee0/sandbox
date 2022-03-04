package com.redhat.service.bridge.integration.tests.steps;

import com.redhat.service.bridge.integration.tests.resources.ActionResource;

import io.cucumber.java.en.Then;
import io.restassured.response.Response;

import static com.redhat.service.bridge.integration.tests.common.ActionUtils.SLACK_CHANNEL;
import static com.redhat.service.bridge.integration.tests.common.ActionUtils.SLACK_TOKEN;
import static com.redhat.service.bridge.integration.tests.common.ActionUtils.SLACK_URI;
import static com.redhat.service.bridge.integration.tests.common.ActionUtils.slackMessage;
import static org.assertj.core.api.Assertions.assertThat;

public class ActionSteps {

    @Then("verify slack message is received in the slack app")
    public void slackActionTest() {
        Response response = ActionResource.getListOfSlackMessageResponse(SLACK_URI + SLACK_CHANNEL, SLACK_TOKEN);
        assertThat(response.getBody().jsonPath().getList("messages.text")).containsAnyOf(slackMessage);
    }
}
