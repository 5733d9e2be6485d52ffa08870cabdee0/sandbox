package com.redhat.service.bridge.integration.tests.common;

import org.keycloak.representations.AccessTokenResponse;

import com.redhat.service.bridge.integration.tests.context.BridgeContext;
import com.redhat.service.bridge.integration.tests.context.TestContext;
import com.redhat.service.bridge.integration.tests.resources.BridgeResource;

import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;

public class BridgeUtils {

    public static final String MANAGER_URL = Utils.getSystemProperty("event-bridge.manager.url");

    protected static final String USER_NAME = Utils.getSystemProperty("bridge.token.username");
    protected static final String PASSWORD = Utils.getSystemProperty("bridge.token.password");
    protected static final String CLIENT_ID = Utils.getSystemProperty("bridge.client.id");
    protected static final String CLIENT_SECRET = Utils.getSystemProperty("bridge.client.secret");

    protected static String token;
    protected static String keycloakURL = System.getProperty("keycloak.realm.url");

    public static String retrieveBridgeToken() {
        if (token == null) {
            String env_token = System.getenv("OB_TOKEN");
            if (env_token != null) {
                token = env_token;
            } else if (keycloakURL != null && !keycloakURL.isEmpty()) {
                token = getAccessToken();
            } else {
                throw new RuntimeException(
                        "Environment variable token and keycloak.realm.url was not defined for token generation.");
            }
        }
        return token;
    }

    private static String getAccessToken() {
        return given().param("grant_type", "password")
                .param("username", USER_NAME)
                .param("password", PASSWORD)
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
