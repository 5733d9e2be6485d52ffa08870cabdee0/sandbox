package com.redhat.service.bridge.integration.tests.resources;

import java.util.Optional;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class ResourceUtils {

    public static RequestSpecification jsonRequest() {
        return jsonRequest(Optional.empty());
    }

    public static RequestSpecification jsonRequest(String token) {
        return jsonRequest(Optional.ofNullable(token));
    }

    public static RequestSpecification jsonRequest(Optional<String> token) {
        RequestSpecification requestSpec = given().contentType(ContentType.JSON).when();
        if (token.isPresent()) {
            return requestSpec.auth().oauth2(token.get());
        } else {
            return requestSpec;
        }
    }
}
