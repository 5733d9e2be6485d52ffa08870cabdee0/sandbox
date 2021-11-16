package com.redhat.service.bridge.actions.webhook;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.actions.ActionInvoker;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebhookInvoker implements ActionInvoker {

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Logger LOG = LoggerFactory.getLogger(WebhookInvoker.class);

    private final String endpoint;
    private final OkHttpClient client;

    public WebhookInvoker(String endpoint) {
        this(endpoint, new OkHttpClient());
    }

    public WebhookInvoker(String endpoint, OkHttpClient client) {
        this.endpoint = endpoint;
        this.client = client;
    }

    @Override
    public void onEvent(String event) {
        Request request = new Request.Builder()
                .url(endpoint)
                .post(RequestBody.create(JSON, event))
                .build();

        try (Response response = client.newCall(request).execute()) {
            LOG.trace("Sent event to {} (response code {})", endpoint, response.code());
        } catch (IOException e) {
            LOG.error("IOException when sending event to " + endpoint, e);
        }
    }
}
