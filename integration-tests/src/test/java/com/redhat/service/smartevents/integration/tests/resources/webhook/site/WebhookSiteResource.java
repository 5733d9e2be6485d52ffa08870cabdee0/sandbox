package com.redhat.service.smartevents.integration.tests.resources.webhook.site;

import java.util.List;

import com.redhat.service.smartevents.integration.tests.common.Utils;

import io.restassured.RestAssured;

public class WebhookSiteResource {

    private static final String ENDPOINT_BASE_URL = "https://webhook.site";
    private static final String ENDPOINT_UUID = "webhook.site.uuid";

    public static List<WebhookSiteRequest> requests(WebhookSiteQuerySorting sorting) {
        return RestAssured.get(ENDPOINT_BASE_URL + "/token/{webhookUuid}/requests?sorting={sorting}", getEndpointUuid(), sorting.getValue())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath().getList("data", WebhookSiteRequest.class);
    }

    public static void deleteRequest(final WebhookSiteRequest request) {
        RestAssured
                .given()
                .delete(ENDPOINT_BASE_URL + "/token/{webhookUuid}/request/{requestUuid}",
                        getEndpointUuid(),
                        request.getUuid())
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    private static String getEndpointUuid() {
        return Utils.getSystemProperty(ENDPOINT_UUID);
    }

    public static boolean isSpecified() {
        String endpointUuid = System.getProperty(ENDPOINT_UUID);
        return endpointUuid != null && !endpointUuid.isBlank();
    }
}
