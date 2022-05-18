package com.redhat.service.smartevents.processor.actions.webhook;

import com.redhat.service.smartevents.infra.auth.OidcClient;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public class WebhookActionInvoker implements ActionInvoker {

    private final String endpoint;
    private final WebClient webClient;
    private final OidcClient oidcClient;
    private final String basicAuthUsername;
    private final String basicAuthPassword;

    public WebhookActionInvoker(String endpoint, WebClient webClient) {
        this(endpoint, webClient, null, null, null);
    }

    public WebhookActionInvoker(String endpoint, WebClient webClient, OidcClient oidcClient) {
        this(endpoint, webClient, oidcClient, null, null);
    }

    public WebhookActionInvoker(String endpoint, WebClient webClient, String basicAuthUsername, String basicAuthPassword) {
        this(endpoint, webClient, null, basicAuthUsername, basicAuthPassword);
    }

    private WebhookActionInvoker(String endpoint, WebClient webClient, OidcClient oidcClient, String basicAuthUsername, String basicAuthPassword) {
        this.endpoint = endpoint;
        this.webClient = webClient;
        this.oidcClient = oidcClient;
        this.basicAuthUsername = basicAuthUsername;
        this.basicAuthPassword = basicAuthPassword;
    }

    @Override
    public void onEvent(String event) {
        HttpRequest<Buffer> request = webClient.postAbs(endpoint);
        if (oidcClient != null) {
            String token = oidcClient.getToken();
            if (token != null && !"".equals(token)) {
                request = request.bearerTokenAuthentication(token);
                request = request.putHeader("content-type", "application/cloudevents+json"); // TODO: refactor
            }
        } else if (basicAuthUsername != null) {
            request.basicAuthentication(basicAuthUsername, basicAuthPassword);
        }
        request.sendJsonObjectAndForget(new JsonObject(event));
    }

    String getEndpoint() {
        return endpoint;
    }

    WebClient getWebClient() {
        return webClient;
    }

    OidcClient getOidcClient() {
        return oidcClient;
    }

    String getBasicAuthUsername() {
        return basicAuthUsername;
    }

    String getBasicAuthPassword() {
        return basicAuthPassword;
    }
}
