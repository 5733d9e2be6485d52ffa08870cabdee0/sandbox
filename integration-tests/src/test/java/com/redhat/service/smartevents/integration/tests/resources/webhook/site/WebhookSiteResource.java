package com.redhat.service.smartevents.integration.tests.resources.webhook.site;

import java.util.List;

import io.restassured.RestAssured;

public class WebhookSiteResource {

    private static final String ENDPOINT_BASE_URL = "https://webhook.site";
    private static final String ENDPOINT_UUID = "webhook.site.uuid";
    //private static final String ENDPOINT_TEST_UUID = Utils.getSystemProperty("webhook.site.uuid.second");

    public static List<WebhookSiteRequest> requests(String webhookID, WebhookSiteQuerySorting sorting) {
        return RestAssured.get(ENDPOINT_BASE_URL + "/token/{webhookUuid}/requests?sorting={sorting}", webhookID, sorting.getValue())
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .body()
                .jsonPath().getList("data", WebhookSiteRequest.class);
    }

    public static void deleteRequest(final WebhookSiteRequest request, Object webhookID) {
        RestAssured
                .given()
                .delete(ENDPOINT_BASE_URL + "/token/{webhookUuid}/request/{requestUuid}",
                        webhookID,
                        request.getUuid())
                .then()
                .log().ifValidationFails()
                .statusCode(200);
    }

    /*
     * private static String getEndpointUuid() {
     * return Utils.getSystemProperty(ENDPOINT_UUID);
     * }
     */

    public static boolean isSpecified() {
        String endpointUuid = System.getProperty(ENDPOINT_UUID);
        return endpointUuid != null && !endpointUuid.isBlank();
    }
}
