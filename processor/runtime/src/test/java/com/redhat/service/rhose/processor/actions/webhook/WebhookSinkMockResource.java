package com.redhat.service.rhose.processor.actions.webhook;

import com.redhat.service.rhose.test.wiremock.AbstractWireMockResourceManager;

public class WebhookSinkMockResource extends AbstractWireMockResourceManager {

    public WebhookSinkMockResource() {
        super("test.webhookSinkUrl");
    }
}
