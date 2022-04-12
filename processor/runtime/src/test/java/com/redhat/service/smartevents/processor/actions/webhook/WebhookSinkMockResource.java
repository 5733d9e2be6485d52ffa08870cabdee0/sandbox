package com.redhat.service.smartevents.processor.actions.webhook;

import com.redhat.service.smartevents.test.wiremock.AbstractWireMockResourceManager;

public class WebhookSinkMockResource extends AbstractWireMockResourceManager {

    public WebhookSinkMockResource() {
        super("test.webhookSinkUrl");
    }
}
