package com.redhat.service.bridge.manager.utils;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;

import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class TestUtils {

    private static RequestSpecification jsonRequest() {
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when();
    }

    public static Response getBridges() {
        return jsonRequest()
                .get(APIConstants.USER_API_BASE_PATH);
    }

    public static Response getBridge(String id) {
        return jsonRequest()
                .get(APIConstants.USER_API_BASE_PATH + id);
    }

    public static Response getProcessor(String bridgeId, String processorId) {
        return jsonRequest()
                .get(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/" + processorId);
    }

    public static Response addProcessorToBridge(String bridgeId, ProcessorRequest p) {
        return jsonRequest()
                .body(p)
                .post(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/");
    }

    public static Response createBridge(BridgeRequest request) {
        return jsonRequest()
                .body(request)
                .post(APIConstants.USER_API_BASE_PATH);
    }

    public static Response deleteBridge(String id) {
        return jsonRequest()
                .delete(APIConstants.USER_API_BASE_PATH + id);
    }

    public static Response getBridgesToDeployOrDelete() {
        return jsonRequest()
                .get(APIConstants.SHARD_API_BASE_PATH);
    }

    public static Response updateBridge(BridgeDTO bridgeDTO) {
        return jsonRequest()
                .body(bridgeDTO)
                .put(APIConstants.SHARD_API_BASE_PATH);
    }

    public static Response getProcessorsToDeployOrDelete() {
        return jsonRequest()
                .get(APIConstants.SHARD_API_BASE_PATH + "processors");
    }

    public static Response updateProcessor(ProcessorDTO processorDTO) {
        return jsonRequest()
                .body(processorDTO)
                .put(APIConstants.SHARD_API_BASE_PATH + "processors");
    }
}
