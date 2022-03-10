package com.redhat.service.bridge.integration.tests.resources;

import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class SlackResource {

    public static Response getListOfSlackMessageResponse(String URI, String token) {
        return given().header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .when()
                .get(URI);
    }
}
