package com.redhat.service.smartevents.integration.tests.resources;

import java.io.InputStream;

import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;
import com.redhat.service.smartevents.integration.tests.common.BridgeUtils;
import com.redhat.service.smartevents.integration.tests.common.Constants;
import com.redhat.service.smartevents.manager.v1.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v1.api.models.responses.BridgeListResponse;
import com.redhat.service.smartevents.manager.v1.api.models.responses.BridgeResponse;

import io.restassured.response.Response;

public class BridgeResource {

    public static BridgeResponse addBridge(String token, String bridgeName, String cloudProvider, String region) {
        return addBridgeResponse(token, bridgeName, cloudProvider, region)
                .then()
                .log().ifValidationFails()
                .statusCode(202)
                .extract()
                .as(BridgeResponse.class);
    }

    public static BridgeResponse addBridge(String token, InputStream bridgeRequest) {
        return addBridgeResponse(token, bridgeRequest)
                .then()
                .log().ifValidationFails()
                .statusCode(202)
                .extract()
                .as(BridgeResponse.class);
    }

    public static Response addBridgeResponse(String token, String bridgeName, String cloudProvider, String region) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .body(new BridgeRequest(bridgeName, cloudProvider, region))
                .post(BridgeUtils.MANAGER_URL + V1APIConstants.V1_USER_API_BASE_PATH);
    }

    public static Response addBridgeResponse(String token, InputStream bridgeRequest) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .body(bridgeRequest)
                .post(BridgeUtils.MANAGER_URL + V1APIConstants.V1_USER_API_BASE_PATH);
    }

    public static Response updateBridgeResponse(String token, String bridgeId, InputStream bridgeRequest) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .body(bridgeRequest).put(BridgeUtils.MANAGER_URL + V1APIConstants.V1_USER_API_BASE_PATH + bridgeId);
    }

    public static BridgeResponse updateBridge(String token, String bridgeId, InputStream bridgeRequest) {
        return updateBridgeResponse(token, bridgeId, bridgeRequest).then()
                .log().ifValidationFails()
                .statusCode(202)
                .extract()
                .as(BridgeResponse.class);
    }

    public static Response getBridgeDetailsResponse(String token, String bridgeId) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .get(BridgeUtils.MANAGER_URL + V1APIConstants.V1_USER_API_BASE_PATH + bridgeId);
    }

    public static BridgeResponse getBridgeDetails(String token, String bridgeId) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .get(BridgeUtils.MANAGER_URL + V1APIConstants.V1_USER_API_BASE_PATH + bridgeId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .as(BridgeResponse.class);
    }

    public static Response getBridgeErrorHandlerResponse(String token, String errorHandlerEndpoint) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .get(errorHandlerEndpoint);
    }

    public static BridgeListResponse getBridgeList(String token) {
        return getBridgeListResponse(token)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .as(BridgeListResponse.class);
    }

    public static Response getBridgeListResponse(String token) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .get(BridgeUtils.MANAGER_URL + V1APIConstants.V1_USER_API_BASE_PATH);
    }

    public static void deleteBridge(String token, String bridgeId) {
        deleteBridgeResponse(token, bridgeId)
                .then()
                .log().ifValidationFails()
                .statusCode(202);
    }

    public static Response deleteBridgeResponse(String token, String bridgeId) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .delete(BridgeUtils.MANAGER_URL + V1APIConstants.V1_USER_API_BASE_PATH + bridgeId);
    }
}
