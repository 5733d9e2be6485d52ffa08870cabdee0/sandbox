package com.redhat.service.smartevents.integration.tests.common;

import org.keycloak.representations.AccessTokenResponse;

import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;

public class BridgeUtils {

    protected static final String CLIENT_ID = System.getProperty("bridge.client.id");
    protected static final String CLIENT_SECRET = System.getProperty("bridge.client.secret");
    protected static final String OB_TOKEN = System.getenv("OB_TOKEN");
    protected static final String OPENSHIFT_OFFLINE_TOKEN = System.getenv("OPENSHIFT_OFFLINE_TOKEN");

    protected static String keycloakURL = System.getProperty("keycloak.realm.url");

    public static String retrieveBridgeToken() {
        if (keycloakURL != null && !keycloakURL.isEmpty() && OPENSHIFT_OFFLINE_TOKEN != null) {
            return refreshAccessToken();
        } else if (OB_TOKEN != null && !OB_TOKEN.isEmpty()) {
            return OB_TOKEN;
        } else if (keycloakURL != null && !keycloakURL.isEmpty()) {
            return getAccessToken();
        } else {
            throw new RuntimeException(
                    "Environment variable token and keycloak.realm.url was not defined for token generation.");
        }
    }

    private static String refreshAccessToken() {
        if (CLIENT_ID.isEmpty() || OPENSHIFT_OFFLINE_TOKEN.isEmpty()) {
            throw new RuntimeException("Client_credentials were not defined for token refresh.");
        }
        return given().param("grant_type", "refresh_token")
                .param("client_id", CLIENT_ID)
                .param("refresh_token", OPENSHIFT_OFFLINE_TOKEN)
                .contentType("application/x-www-form-urlencoded")
                .accept(ContentType.JSON)
                .when()
                .post(keycloakURL + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class)
                .getToken();
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
}
