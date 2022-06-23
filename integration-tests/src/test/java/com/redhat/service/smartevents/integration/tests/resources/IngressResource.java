package com.redhat.service.smartevents.integration.tests.resources;

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
            return ResourceUtils.newRequest(token, ContentType.JSON.toString())
                    .body(cloudEventStream)
                    .options(endpoint);
        } catch (IOException e) {
            throw new RuntimeException("Error with inputstream", e);
        }
    }

    public static Response postCloudEventResponse(String token, String endpoint, InputStream cloudEventStream,
            Headers headers, String contentType) {
        return ResourceUtils.newRequest(token, contentType)
                .headers(headers)
                .body(cloudEventStream)
                .post(endpoint);
    }
}
