package com.redhat.service.bridge.integration.tests.common;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeListResponse;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;

import io.restassured.response.Response;

public class BridgeCommon extends AbstractBridge {

    public static BridgeResponse addBridge(String bridgeName) {
        return jsonRequestWithAuth()
                .body(new BridgeRequest(bridgeName))
                .post(managerUrl + APIConstants.USER_API_BASE_PATH)
                .then()
                .statusCode(201)
                .extract()
                .as(BridgeResponse.class);
    }

    public static Response getBridgeDetails(String bridgeId) {
        return jsonRequestWithAuth()
                .get(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId);
    }

    public static BridgeListResponse getBridgeList() {
        return jsonRequestWithAuth()
                .get(managerUrl + APIConstants.USER_API_BASE_PATH)
                .then()
                .statusCode(200)
                .extract()
                .as(BridgeListResponse.class);
    }

    public static void deleteBridge(String bridgeId) {
        jsonRequestWithAuth()
                .delete(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId)
                .then()
                .statusCode(202);
    }

    public static ProcessorResponse createProcessor(String bridgeId, ProcessorRequest processorRequest) {
        return jsonRequestWithAuth()
                .body(processorRequest)
                .post(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors")
                .then()
                .statusCode(201)
                .extract()
                .as(ProcessorResponse.class);
    }

    public static Response getProcessor(String bridgeId, String processorId) {
        return jsonRequestWithAuth()
                .get(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/" + processorId);
    }

    public static void deleteProcessor(String bridgeId, String processorId) {
        jsonRequestWithAuth()
                .delete(managerUrl + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/" + processorId)
                .then()
                .statusCode(202);
    }
}
