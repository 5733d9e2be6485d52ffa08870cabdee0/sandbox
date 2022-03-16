package com.redhat.service.bridge.integration.tests.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.restassured.http.ContentType;
import io.restassured.http.Headers;
import io.restassured.response.Response;

public class IngressResource {

    public static Response optionsJsonEmptyEventResponse(String token, String endpoint) {
        try (ByteArrayInputStream cloudEventStream = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))) {
            return ResourceUtils.jsonRequest(token)
                    .body(cloudEventStream)
                    .contentType(ContentType.JSON)
                    .options(endpoint + "/events");
        } catch (IOException e) {
            throw new RuntimeException("Error with inputstream", e);
        }
    }

    public static Response postCloudEventResponse(String token, String endpoint, InputStream cloudEventStream,
            Headers headers) {
        return ResourceUtils.jsonRequest(token)
                .headers(headers)
                .body(cloudEventStream)
                .contentType(ContentType.JSON)
                .post(endpoint);
    }
}
