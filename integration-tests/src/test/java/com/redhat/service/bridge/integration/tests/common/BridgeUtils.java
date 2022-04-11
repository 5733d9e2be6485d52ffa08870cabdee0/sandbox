package com.redhat.service.bridge.integration.tests.common;

import org.keycloak.representations.AccessTokenResponse;

import com.redhat.service.bridge.integration.tests.context.BridgeContext;
import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.resources.BridgeResource;

import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;

public class BridgeUtils {

    public static final String MANAGER_URL = System.getProperty("event-bridge.manager.url");
    protected static final String CLIENT_ID = System.getProperty("bridge.client.id");
    protected static final String CLIENT_SECRET = System.getProperty("bridge.client.secret");

    protected static String keycloakURL = System.getProperty("keycloak.realm.url");

    public static String retrieveBridgeToken() {
        String env_token = System.getenv("OB_TOKEN");
        if (env_token != null) {
            return env_token;
        } else if (keycloakURL != null && !keycloakURL.isEmpty()) {
            return getAccessToken();
        } else {
            throw new RuntimeException(
                    "Environment variable token and keycloak.realm.url was not defined for token generation.");
        }
    }

    private static String getAccessToken() {
        if (CLIENT_ID.isEmpty() || CLIENT_SECRET.isEmpty()) {
            throw new RuntimeException("Client_credentials were not defined for token generation.");
        }
        return given().param("grant_type", "client_credentials")
                .param("client_id", CLIENT_ID)
                .param("client_secret", CLIENT_SECRET)
                .contentType("application/x-www-form-urlencoded")
                .accept(ContentType.JSON)
                .when()
                .post(keycloakURL + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class)
                .getToken();
    }

    public static String getOrRetrieveBridgeEndpoint(TestContext context, String testBridgeName) {
        String bridgeEventsEndpoint = getOrRetrieveBridgeEventsEndpoint(context, testBridgeName);
        return bridgeEventsEndpoint.replaceFirst("/events$", "");
    }

    public static String getOrRetrieveBridgeEventsEndpoint(TestContext context, String testBridgeName) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        if (bridgeContext.getEndPoint() == null) {
            // store bridge endpoint details
            String endPoint = BridgeResource.getBridgeDetails(context.getManagerToken(), bridgeContext.getId())
                    .getEndpoint();
            // If an endpoint contains localhost without port then default port has to be
            // defined, otherwise rest-assured will use port 8080
            if (endPoint.matches("http://localhost/.*")) {
                endPoint = endPoint.replace("http://localhost/", "http://localhost:80/");
            }
            bridgeContext.setEndPoint(endPoint);
        }

        return bridgeContext.getEndPoint();
    }
}
