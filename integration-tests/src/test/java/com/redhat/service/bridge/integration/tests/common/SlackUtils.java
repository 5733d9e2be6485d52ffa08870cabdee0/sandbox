package com.redhat.service.bridge.integration.tests.common;

import io.vertx.core.json.JsonObject;

public class SlackUtils {

    public final static String SLACK_URI = "https://slack.com/api/conversations.history";
    public final static String SLACK_CHANNEL = "?channel=C0346PP8EL8";

    private final static String SLACK_TOKEN = Utils.getSystemProperty("slack.webhook.token");
    private final static String SLACK_WEBHOOK_URL = Utils.getSystemProperty("slack.webhook.url");

    public static String slackMessage;

    public static String setAndRetrieveSlackMessageCloudEvent(String cloudEvent) {
        slackMessage = "Test Slack message - " + Utils.getTodayDateTime();

        JsonObject json = new JsonObject(cloudEvent);
        json.getJsonObject("data").put("myMessage", slackMessage);
        return json.toString();
    }

    public static String getSlackToken() {
        return SLACK_TOKEN;
    }

    public static String setAndRetrieveSlackProcessorPayload(String processorRequestJson) {
        JsonObject json = new JsonObject(processorRequestJson);
        json.getJsonObject("action").getJsonObject("parameters").put("webhookUrl", SLACK_WEBHOOK_URL);
        return json.toString();
    }
}
