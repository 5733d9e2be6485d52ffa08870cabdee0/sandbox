package com.redhat.service.smartevents.integration.tests.resources.webhook.performance;

import java.util.Optional;

import com.redhat.service.smartevents.integration.tests.common.Constants;
import com.redhat.service.smartevents.integration.tests.resources.ResourceUtils;

import io.restassured.response.Response;

public class WebhookPerformanceResource {

    private static final String BASE_ENDPOINT_URL = System.getProperty("performance.slack.webhook.url");

    public static long getAllEventsCount() {
        return getAllEventsResponse()
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath().getList("id").size();
    }

    public static Response getAllEventsResponse() {
        return ResourceUtils.newRequest(Optional.empty(), Constants.JSON_CONTENT_TYPE)
                .get(BASE_ENDPOINT_URL);
    }

    public static Response deleteAllResponse() {
        return ResourceUtils.newRequest(Optional.empty(), Constants.JSON_CONTENT_TYPE)
                .delete(BASE_ENDPOINT_URL);
    }

    public static void deleteAll() {
        deleteAllResponse()
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    public static boolean isSpecified() {
        return BASE_ENDPOINT_URL != null && !BASE_ENDPOINT_URL.isBlank();
    }
}
