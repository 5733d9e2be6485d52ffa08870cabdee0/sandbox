package com.redhat.service.smartevents.integration.tests.resources.webhook.site;

import java.util.List;

import com.redhat.service.smartevents.integration.tests.common.Utils;

import io.restassured.RestAssured;

public class WebhookSiteResource {

    private static final String ENDPOINT_BASE_URL = "https://webhook.site";

    private static final String ENDPOINT_UUID = Utils.getSystemProperty("webhook.site.uuid");

    public static List<WebhookSiteRequest> requests() {
        return RestAssured.get(ENDPOINT_BASE_URL + "/token/{webhookUuid}/requests", ENDPOINT_UUID)
                .then()
                .extract()
                .body()
                .jsonPath().getList("data", WebhookSiteRequest.class);
    }

    public static void deleteRequest(final WebhookSiteRequest request) {
        RestAssured
                .given()
                .delete(ENDPOINT_BASE_URL + "/token/{webhookUuid}/request/{requestUuid}",
                        ENDPOINT_UUID,
                        request.getUuid())
                .then()
                .statusCode(200);
    }
}
