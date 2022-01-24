package com.redhat.service.bridge.integration.tests.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.keycloak.representations.AccessTokenResponse;

import com.redhat.service.bridge.test.resource.KeycloakResource;

import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public abstract class AbstractBridge {

    private static final String USER_NAME = "kermit";
    private static final String PASSWORD = "thefrog";

    protected static String token;
    protected static Properties prop;
    protected static String managerUrl;
    protected static String keycloakURL;

    static {
        loadPropertiesFile();
        managerUrl = prop.getProperty("event-bridge.manager.url");
        keycloakURL = prop.getProperty("key-cloak.url");
    }

    private static void loadPropertiesFile() {
        final String filePath = System.getenv("PWD") + "/src/test/resources/application.properties";
        prop = new Properties();
        try {
            InputStream in = new FileInputStream(filePath);
            prop.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static RequestSpecification jsonRequestWithAuth() {
        if (token == null) {
            token = getAccessToken();
        }
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .auth()
                .oauth2(token);
    }

    private static String getAccessToken() {
        return given().param("grant_type", "password")
                .param("username", AbstractBridge.USER_NAME)
                .param("password", AbstractBridge.PASSWORD)
                .param("client_id", KeycloakResource.CLIENT_ID)
                .param("client_secret", KeycloakResource.CLIENT_SECRET)
                .when()
                .post(keycloakURL + "/protocol/openid-connect/token")
                .as(AccessTokenResponse.class)
                .getToken();
    }
}
