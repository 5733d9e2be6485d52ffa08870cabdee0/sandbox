package com.redhat.service.bridge.integration.tests.resources;

import java.util.List;

import com.redhat.service.bridge.integration.tests.common.Utils;

import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;

public class SlackResource {

    private final static String SLACK_URI = "https://slack.com/api/conversations.history?channel={channel}";
    private final static String SLACK_CHANNEL = Utils.getSystemProperty("slack.channel");
    private final static String SLACK_TOKEN = Utils.getSystemProperty("slack.webhook.token");

    public static List<String> getListOfSlackMessages() {
        return given()
                .auth()
                .oauth2(SLACK_TOKEN)
                .contentType(ContentType.JSON)
                .when()
                .get(SLACK_URI, SLACK_CHANNEL)
                .getBody()
                .jsonPath()
                .getList("messages.text");
    }
}
