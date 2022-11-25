package com.redhat.service.smartevents.integration.tests.v2.resources;

import java.io.InputStream;

import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.integration.tests.common.Constants;
import com.redhat.service.smartevents.integration.tests.resources.ResourceUtils;
import com.redhat.service.smartevents.integration.tests.v2.common.BridgeUtils;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorListResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorResponse;

import io.restassured.response.Response;

public class ProcessorResource {

    public static Response createProcessorResponse(String token, String bridgeId, InputStream processorRequest) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .body(processorRequest)
                .post(BridgeUtils.MANAGER_URL + V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors");
    }

    public static ProcessorResponse createProcessor(String token, String bridgeId, InputStream processorRequest) {
        return createProcessorResponse(token, bridgeId, processorRequest)
                .then()
                .log().ifValidationFails()
                .statusCode(202)
                .extract()
                .as(ProcessorResponse.class);
    }

    public static Response updateProcessorResponse(String token, String bridgeId, String processorId, InputStream processorRequest) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .body(processorRequest)
                .put(BridgeUtils.MANAGER_URL + V2APIConstants.V2_USER_API_BASE_PATH + "{bridgeId}/processors/{processorId}", bridgeId, processorId);
    }

    public static ProcessorResponse updateProcessor(String token, String bridgeId, String processorId, InputStream processorRequest) {
        return updateProcessorResponse(token, bridgeId, processorId, processorRequest)
                .then()
                .log().ifValidationFails()
                .statusCode(202)
                .extract()
                .as(ProcessorResponse.class);
    }

    public static ProcessorResponse getProcessor(String token, String bridgeId, String processorId) {
        return getProcessorResponse(token, bridgeId, processorId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .as(ProcessorResponse.class);
    }

    public static Response getProcessorResponse(String token, String bridgeId, String processorId) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .get(BridgeUtils.MANAGER_URL + V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors/"
                        + processorId);
    }

    public static void deleteProcessor(String token, String bridgeId, String processorId) {
        deleteProcessorResponse(token, bridgeId, processorId)
                .then()
                .log().ifValidationFails()
                .statusCode(202);
    }

    public static Response deleteProcessorResponse(String token, String bridgeId, String processorId) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .delete(BridgeUtils.MANAGER_URL + V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors/"
                        + processorId);
    }

    public static ProcessorListResponse getProcessorList(String token, String bridgeId) {
        return getProcessorListResponse(token, bridgeId)
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .extract()
                .as(ProcessorListResponse.class);
    }

    public static Response getProcessorListResponse(String token, String bridgeId) {
        return ResourceUtils.newRequest(token, Constants.JSON_CONTENT_TYPE)
                .get(BridgeUtils.MANAGER_URL + V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors/");
    }
}
