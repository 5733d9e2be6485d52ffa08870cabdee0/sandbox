package com.redhat.service.bridge.integration.tests.resources;

import java.io.InputStream;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.integration.tests.common.BridgeUtils;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorListResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;

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
                .statusCode(201)
                .extract()
                .as(ProcessorResponse.class);
    }

    public static ProcessorResponse getProcessor(String token, String bridgeId, String processorId) {
        return getProcessorResponse(token, bridgeId, processorId)
                .then()
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
        ResourceUtils.jsonRequest(token)
                .delete(BridgeUtils.MANAGER_URL + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/"
                        + processorId)
                .then()
                .statusCode(202);
    }

    public static ProcessorListResponse getProcessorList(String token, String bridgeId) {
        return getProcessorListResponse(token, bridgeId)
                .then()
                .extract()
                .as(ProcessorListResponse.class);
    }

    public static Response getProcessorListResponse(String token, String bridgeId) {
        return ResourceUtils.jsonRequest(token)
                .get(BridgeUtils.MANAGER_URL + APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/");
    }
}
