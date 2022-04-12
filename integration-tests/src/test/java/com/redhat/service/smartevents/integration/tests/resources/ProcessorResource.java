package com.redhat.service.smartevents.integration.tests.resources;

import java.io.InputStream;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.integration.tests.common.BridgeUtils;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorListResponse;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorResponse;

import io.restassured.response.Response;

public class ProcessorResource {

    public static Response createProcessorResponse(String token, String bridgeId, InputStream processorRequest) {
        return ResourceUtils.jsonRequest(token)
                .body(processorRequest)
                .post(BridgeUtils.MANAGER_URL + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors");
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
        return ResourceUtils.jsonRequest(token)
                .body(processorRequest)
                .put(BridgeUtils.MANAGER_URL + APIConstants.USER_API_BASE_PATH + "{bridgeId}/processors/{processorId}", bridgeId, processorId);
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
        return ResourceUtils.jsonRequest(token)
                .get(BridgeUtils.MANAGER_URL + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/"
                        + processorId);
    }

    public static void deleteProcessor(String token, String bridgeId, String processorId) {
        deleteProcessorResponse(token, bridgeId, processorId)
                .then()
                .log().ifValidationFails()
                .statusCode(202);
    }

    public static Response deleteProcessorResponse(String token, String bridgeId, String processorId) {
        return ResourceUtils.jsonRequest(token)
                .delete(BridgeUtils.MANAGER_URL + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/"
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
        return ResourceUtils.jsonRequest(token)
                .get(BridgeUtils.MANAGER_URL + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/");
    }
}
