package com.redhat.service.bridge.actions.webhook;

import com.redhat.service.bridge.test.wiremock.AbstractWireMockResourceManager;

public class WebhookSinkMockResource extends AbstractWireMockResourceManager {

    public WebhookSinkMockResource() {
        super("test.webhookSinkUrl");
    }
}
