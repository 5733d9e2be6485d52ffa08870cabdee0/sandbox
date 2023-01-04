package com.redhat.service.smartevents.manager.v2.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ResourceStatusDTO;
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

    public static Response listBridges() {
        return jsonRequest()
                .get(V2APIConstants.V2_USER_API_BASE_PATH);
    }

    public static Response listBridges(int page, int size) {
        return jsonRequest()
                .get(V2APIConstants.V2_USER_API_BASE_PATH + "?size=" + size + "&page=" + page);
    }

    public static Response listBridgesFilterByName(String name) {
        return jsonRequest()
                .get(V2APIConstants.V2_USER_API_BASE_PATH + "?name=" + name);
    }

    public static Response listBridgesFilterByStatus(ManagedResourceStatus... status) {
        String queryString = Arrays.stream(status).map(s -> "status=" + s.getValue()).collect(Collectors.joining("&"));
        return jsonRequest().get(V2APIConstants.V2_USER_API_BASE_PATH + "?" + queryString);
    }

    public static Response listBridgesFilterByStatusWithAnyValue(String... status) {
        String queryString = Arrays.stream(status).map(s -> "status=" + s).collect(Collectors.joining("&"));
        return jsonRequest().get(V2APIConstants.V2_USER_API_BASE_PATH + "?" + queryString);
    }

    public static Response listBridgesFilterByNameAndStatus(String name, ManagedResourceStatus... status) {
        String queryString = Arrays.stream(status).map(s -> "status=" + s.getValue()).collect(Collectors.joining("&"));
        return jsonRequest().get(V2APIConstants.V2_USER_API_BASE_PATH + "?name=" + name + "&" + queryString);
    }

    public static Response getBridge(String id) {
        return jsonRequest()
                .get(V2APIConstants.V2_USER_API_BASE_PATH + id);
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

    public static Response deleteBridge(String id) {
        return jsonRequest()
                .delete(V2APIConstants.V2_USER_API_BASE_PATH + id);
    }

    public static Response listProcessors(String bridgeId, int page, int size) {
        return jsonRequest()
                .get(V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors?size=" + size + "&page=" + page);
    }

    public static Response listProcessorsFilterByName(String bridgeId, String name) {
        return jsonRequest()
                .get(V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors?name=" + name);
    }

    public static Response listProcessorsFilterByStatus(String bridgeId, ManagedResourceStatus... status) {
        String queryString = Arrays.stream(status).map(s -> "status=" + s.getValue()).collect(Collectors.joining("&"));
        return jsonRequest().get(V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors?" + queryString);
    }

    public static Response listProcessorsFilterByStatusWithAnyValue(String bridgeId, String... status) {
        String queryString = Arrays.stream(status).map(s -> "status=" + s).collect(Collectors.joining("&"));
        return jsonRequest().get(V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors/?" + queryString);
    }

    public static Response listProcessorsFilterByNameAndStatus(String bridgeId, String name, ManagedResourceStatus... status) {
        String queryString = Arrays.stream(status).map(s -> "status=" + s.getValue()).collect(Collectors.joining("&"));
        return jsonRequest()
                .get(V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors?name=" + name + "&" + queryString);
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

    public static Response updateProcessor(String bridgeId, String processorId, ProcessorRequest p) {
        return jsonRequest()
                .body(p)
                .put(V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors/" + processorId);
    }

    public static Response deleteProcessor(String bridgeId, String processorId) {
        return jsonRequest()
                .delete(V2APIConstants.V2_USER_API_BASE_PATH + bridgeId + "/processors/" + processorId);
    }

    public static Response getBridgesToDeployOrDelete() {
        return jsonRequest()
                .get(V2APIConstants.V2_SHARD_API_BASE_PATH);
    }

    public static Response getProcessorsToDeployOrDelete() {
        return jsonRequest()
                .get(V2APIConstants.V2_SHARD_API_BASE_PATH + "processors");
    }

    public static Response updateBridgeStatus(ResourceStatusDTO statusDTO) {
        return updateBridgesStatus(List.of(statusDTO));
    }

    public static Response updateBridgesStatus(List<ResourceStatusDTO> statusDTOs) {
        return jsonRequest()
                .body(statusDTOs)
                .put(V2APIConstants.V2_SHARD_API_BASE_PATH);
    }

    public static Response updateProcessorStatus(ResourceStatusDTO statusDTO) {
        return updateProcessorsStatus(List.of(statusDTO));
    }

    public static Response updateProcessorsStatus(List<ResourceStatusDTO> statusDTOs) {
        return jsonRequest()
                .body(statusDTOs)
                .put(V2APIConstants.V2_SHARD_API_BASE_PATH + "processors");
    }
}
