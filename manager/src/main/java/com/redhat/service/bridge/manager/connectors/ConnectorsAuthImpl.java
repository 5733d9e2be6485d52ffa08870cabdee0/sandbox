package com.redhat.service.bridge.manager.connectors;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.scheduler.Scheduled;
import io.vertx.core.json.JsonObject;

@RequestScoped
public class ConnectorsAuthImpl implements ConnectorsAuth {

    @ConfigProperty(name = "managed-connectors.auth.server-url")
    String authServerUrl;

    @ConfigProperty(name = "managed-connectors.auth.client-id")
    String clientId;

    @ConfigProperty(name = "managed-connectors.auth.token-path")
    String tokenPath;

    @ConfigProperty(name = "managed-connectors.auth.offline-token")
    String offlineToken;

    String bearerToken;

    @Override
    public String bearerToken() {
        if (bearerToken == null) {
            bearerToken = bearerTokenFromOfflineToken(offlineToken);
        }
        return bearerToken;
    }

    @Scheduled(every = "5m")
    void resetBearerToken() {
        bearerToken = null;
    }

    /**
     * Copied from https://github.com/redhat-developer/app-services-operator/blob/main/source/rhoas/src/main/java/com/openshift/cloud/beans/AccessTokenSecretTool.java#L117
     * This method exchanges an offline token for a new refresh token
     *
     * @param offlineToken the token from ss.redhat.com
     * @return a token to be used as a bearer token to authorize the user
     */
    private String bearerTokenFromOfflineToken(String offlineToken) {
        try {
            HttpRequest request =
                    HttpRequest.newBuilder().uri(URI.create(authServerUrl + "/" + tokenPath))
                            .header("content-type", "application/x-www-form-urlencoded")
                            .timeout(Duration.ofMinutes(2)).POST(ofFormData("grant_type", "refresh_token",
                                    "client_id", clientId, "refresh_token", offlineToken))
                            .build();

            HttpClient client = HttpClient.newBuilder().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String tokens = response.body();
                JsonObject json = new JsonObject(tokens);
                return json.getString("access_token");
            } else {
                String errorMessage = String.format("Http Error Code %d\n", response.statusCode()) + response.body();
                throw new RuntimeException(errorMessage);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);

        }
    }

    private static HttpRequest.BodyPublisher ofFormData(String... data) {
        StringBuilder builder = new StringBuilder();
        if (data.length % 2 == 1) {
            throw new IllegalArgumentException(
                    "Data must be key value pairs, but an number of data were given. ");
        }

        for (int index = 0; index < data.length; index += 2) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(data[index], StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(data[index + 1], StandardCharsets.UTF_8));
        }

        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }
}
