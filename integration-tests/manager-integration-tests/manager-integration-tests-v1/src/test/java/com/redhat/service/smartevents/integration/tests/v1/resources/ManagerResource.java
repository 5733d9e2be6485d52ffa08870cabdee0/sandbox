package com.redhat.service.smartevents.integration.tests.v1.resources;

import java.util.Optional;

import com.redhat.service.smartevents.integration.tests.common.Constants;
import com.redhat.service.smartevents.integration.tests.resources.ResourceUtils;
import com.redhat.service.smartevents.integration.tests.v1.common.BridgeUtils;

public class ManagerResource {

    public static String getManagerMetrics() {
        return ResourceUtils.newRequest(Optional.empty(), Constants.TEXT_PLAIN_CONTENT_TYPE)
                .get(BridgeUtils.MANAGER_URL + Constants.METRICS_ENDPOINT)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .asString();
    }
}
