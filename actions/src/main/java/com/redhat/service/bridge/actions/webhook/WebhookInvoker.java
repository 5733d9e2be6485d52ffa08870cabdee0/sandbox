package com.redhat.service.bridge.actions.webhook;

import com.redhat.service.bridge.actions.ActionInvoker;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;

public class WebhookInvoker implements ActionInvoker {

    private final String endpoint;
    private final WebClient client;

    public WebhookInvoker(String endpoint, WebClient client) {
        this.endpoint = endpoint;
        this.client = client;
    }

    @Override
    public void onEvent(String event) {
        client.postAbs(endpoint).sendJsonObjectAndForget(new JsonObject(event));
    }
}
