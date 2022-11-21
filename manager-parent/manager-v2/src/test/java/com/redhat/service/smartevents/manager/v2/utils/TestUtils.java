package com.redhat.service.smartevents.manager.v2.utils;

import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;

import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

public class TestUtils {

    public static RequestSpecification jsonRequest() {
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when();
    }

    public static Response createBridge(BridgeRequest request) {
        return jsonRequest()
                .body(request)
                .post(V2APIConstants.V2_USER_API_BASE_PATH);
    }

    public static Response getProcessor(String bridgeId, String processorId) {
        return jsonRequest()
                .get(V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors/" + processorId);
    }

    public static Response addProcessorToBridge(String bridgeId, ProcessorRequest p) {
        return jsonRequest()
                .body(p)
                .post(V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors/");
    }

    public static Response addProcessorToBridgeWithRequestBody(String bridgeId, String processorRequest) {
        return jsonRequest()
                .body(processorRequest)
                .post(V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors/");
    }

}
