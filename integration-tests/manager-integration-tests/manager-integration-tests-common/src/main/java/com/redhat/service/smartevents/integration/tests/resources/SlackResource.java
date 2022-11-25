package com.redhat.service.smartevents.integration.tests.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

import software.tnb.common.service.ServiceFactory;
import software.tnb.slack.account.SlackAccount;
import software.tnb.slack.service.Slack;
import software.tnb.slack.validation.MessageRequestConfig;

public class SlackResource {

    public static Slack slack = ServiceFactory.create(Slack.class);

    // Manually triggering beforeAll and afterAll as these methods are intended to be triggered as JUnit5 Extension, however Cucumber support JUnit5 Extensions.

    @BeforeAll
    public static void beforeAll() throws Exception {
        slack.beforeAll(null);
    }

    @AfterAll
    public static void afterAll() throws Exception {
        slack.afterAll(null);
    }

    public static SlackAccount account() {
        return slack.account();
    }

    public static List<String> getListOfSlackMessages(String channelName) {
        ZonedDateTime oneHourAgo = ZonedDateTime.now(ZoneOffset.UTC).minusHours(1);
        return slack.validation().getMessages(new MessageRequestConfig().setChannelName(channelName).setOldest(Long.toString(oneHourAgo.toEpochSecond())));
    }

    public static void postToSlackWebhookUrl(String message, String channelName) {
        slack.validation().sendMessageToChannelName(message, channelName);
    }
}
