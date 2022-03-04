package com.redhat.service.bridge.integration.tests.common;

import io.vertx.core.json.JsonObject;

public class ActionUtils {

    public final static String SLACK_URI = "https://slack.com/api/conversations.history";
    public final static String SLACK_TOKEN = "xoxb-3121370458964-3137484984228-u7b4MFP7xu8QRGmYswhx8w94";
    public final static String SLACK_CHANNEL = "?channel=C0346PP8EL8";

    public static String slackMessage;

    public static String setAndRetrieveSlackMessageCloudEvent(String cloudEvent) {
        slackMessage = "Test Slack message - " + Utils.getTodayDateTime();

        JsonObject json = new JsonObject(cloudEvent);
        json.getJsonObject("data").put("myMessage", slackMessage);
        return json.toString();
    }
}
