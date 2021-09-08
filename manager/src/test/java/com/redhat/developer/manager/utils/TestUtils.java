package com.redhat.developer.manager.utils;

import com.redhat.developer.infra.dto.BridgeDTO;
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
                .get("/api/v1/bridges");
    }

    public static Response getBridge(String id) {
        return jsonRequest()
                .get("/api/v1/bridges/" + id);
    }

    public static Response addProcessorToBridge(String bridgeId, ProcessorRequest p) {
        return jsonRequest()
                .body(p)
                .post("/api/v1/bridges/" + bridgeId + "/processors/");
    }

    public static Response createBridge(BridgeRequest request) {
        return jsonRequest()
                .body(request)
                .post("/api/v1/bridges");
    }

    public static Response deleteBridge(String id) {
        return jsonRequest()
                .delete("/api/v1/bridges/" + id);
    }

    public static Response getBridgesToDeployOrDelete() {
        return jsonRequest()
                .get("/api/v1/shard/bridges");
    }

    public static Response updateBridge(BridgeDTO bridgeDTO) {
        return jsonRequest()
                .body(bridgeDTO)
                .put("/api/v1/shard/bridges");
    }
}
