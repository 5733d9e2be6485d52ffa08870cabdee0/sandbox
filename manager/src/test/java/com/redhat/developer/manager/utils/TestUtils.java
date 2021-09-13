package com.redhat.developer.manager.utils;

import com.redhat.developer.infra.api.APIConstants;
import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.ProcessorDTO;
import com.redhat.developer.manager.api.models.requests.BridgeRequest;
import com.redhat.developer.manager.api.models.requests.ProcessorRequest;

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

    public static Response getProcessorsToDeployOrDelete(String bridgeId) {
        return jsonRequest()
                .get(APIConstants.SHARD_API_BASE_PATH + bridgeId + "/processors");
    }

    public static Response updateProcessor(String bridgeId, ProcessorDTO processorDTO) {
        return jsonRequest()
                .body(processorDTO)
                .put(APIConstants.SHARD_API_BASE_PATH + bridgeId + "/processors");
    }
}
