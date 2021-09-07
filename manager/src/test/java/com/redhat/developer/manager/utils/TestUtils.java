package com.redhat.developer.manager.utils;

import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.manager.api.models.requests.BridgeRequest;

import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class TestUtils {

    public static Response getBridges() {
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/bridges");
    }

    public static Response getBridge(String id) {
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/bridges/" + id);
    }

    public static Response createBridge(BridgeRequest request) {
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body(request)
                .post("/api/v1/bridges");
    }

    public static Response getBridgesToDeploy() {
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/api/v1/shard/bridges/toDeploy");
    }

    public static Response updateBridge(BridgeDTO bridgeDTO) {
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .body(bridgeDTO)
                .post("/api/v1/shard/bridges/toDeploy");
    }
}
