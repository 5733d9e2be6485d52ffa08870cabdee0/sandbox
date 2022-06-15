package com.redhat.service.smartevents.integration.tests.resources;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.redhat.service.smartevents.integration.tests.common.Utils;

import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;

public class SlackResource {

    private final static String SLACK_URI = "https://slack.com/api/conversations.history";
    private final static String SLACK_CHANNEL = Utils.getSystemProperty("slack.channel");
    private final static String SLACK_TOKEN = Utils.getSystemProperty("slack.webhook.token");
    private final static String SLACK_WEBHOOK = Utils.getSystemProperty("slack.webhook.url");

    public static List<String> getListOfSlackMessages() {
        final LocalDate yesterday = LocalDate.now(ZoneId.systemDefault()).minusDays(1);
        return given()
                .auth()
                .oauth2(SLACK_TOKEN)
                .contentType(ContentType.JSON)
                .queryParam("channel", SLACK_CHANNEL)
                .queryParam("latest", yesterday.toEpochDay())
                .when()
                .get(SLACK_URI)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("messages.text");
    }

    public static int postToSlackWebhookUrl(final String message) {
        return given()
                .auth()
                .oauth2(SLACK_TOKEN)
                .contentType(ContentType.JSON)
                .body("{\"text\": \"" + message + "\"}")
                .when()
                .post(SLACK_WEBHOOK)
                .getStatusCode();
    }
}
