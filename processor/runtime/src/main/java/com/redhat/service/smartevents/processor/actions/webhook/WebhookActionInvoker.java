package com.redhat.service.smartevents.processor.actions.webhook;

import java.util.Map;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.smartevents.infra.auth.OidcClient;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ExternalUserException;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public class WebhookActionInvoker implements ActionInvoker {

    private static final Logger LOG = LoggerFactory.getLogger(WebhookActionInvoker.class);

    public static final String CE_JSON_CONTENT_TYPE = "application/cloudevents+json";

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
    public void onEvent(String event, Map<String, String> headers) {
        HttpRequest<Buffer> request = webClient.postAbs(endpoint);
        // If the oidcClient is set, then the target is a bridge ingress. The content type has to be application/cloudevents+json.
        if (oidcClient != null) {
            String token = oidcClient.getToken();
            if (token != null && !"".equals(token)) {
                request = request.bearerTokenAuthentication(token);
                request = request.putHeader("content-type", CE_JSON_CONTENT_TYPE);
            }
        } else if (basicAuthUsername != null) {
            request.basicAuthentication(basicAuthUsername, basicAuthPassword);
        }

        // add headers as HTTP headers
        for (Map.Entry<String, String> e : headers.entrySet()) {
            request.headers().add("x-" + e.getKey(), e.getValue());
        }

        // See https://issues.redhat.com/browse/MGDOBR-777
        // We're unable to block the Vert.X thread to wait for the response.
        // java.lang.IllegalStateException: The current thread cannot be blocked: vert.x-eventloop-thread-1
        request.sendJson(new JsonObject(event))
                .subscribe()
                .with(response -> {
                    final int statusCode = response.statusCode();
                    if (Response.Status.fromStatusCode(statusCode).getFamily() != Response.Status.Family.SUCCESSFUL) {
                        String message = String.format("Unable to send event to Webhook. Status Code '%s', Status message '%s'.",
                                response.statusCode(),
                                response.statusMessage());
                        LOG.debug(message);
                        throw new ExternalUserException(message);
                    }
                });
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
