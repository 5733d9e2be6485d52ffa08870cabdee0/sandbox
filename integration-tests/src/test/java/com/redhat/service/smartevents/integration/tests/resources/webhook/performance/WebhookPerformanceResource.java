package com.redhat.service.smartevents.integration.tests.resources.webhook.performance;

import java.util.Optional;

import com.redhat.service.smartevents.integration.tests.common.Constants;
import com.redhat.service.smartevents.integration.tests.resources.ResourceUtils;

import io.restassured.response.Response;

public class WebhookPerformanceResource {

    private static final String BASE_ENDPOINT_URL = System.getProperty("performance.webhook.url");

    public static <T extends Number> T getCountEventsReceived(String bridgeId, Class<T> clazz) {
        return getCountEventsReceivedResponse(bridgeId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .body()
                .as(clazz);
    }

    public static Response getCountEventsReceivedResponse(String bridgeId) {
        return ResourceUtils.newRequest(Optional.empty(), Constants.JSON_CONTENT_TYPE)
                .get(BASE_ENDPOINT_URL + "/" + bridgeId + "/count");
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
