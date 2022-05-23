package com.redhat.service.smartevents.integration.tests.resources;

import java.util.Optional;

import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class ResourceUtils {

    public static RequestSpecification jsonRequest(String token, String contentType) {
        return jsonRequest(Optional.ofNullable(token), contentType);
    }

    public static RequestSpecification jsonRequest(Optional<String> token, String contentType) {
        RequestSpecification requestSpec = given().contentType(contentType).when();
        if (token.isPresent()) {
            return requestSpec.auth().oauth2(token.get());
        } else {
            return requestSpec;
        }
    }
}
