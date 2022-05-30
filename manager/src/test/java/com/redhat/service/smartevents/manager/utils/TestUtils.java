package com.redhat.service.smartevents.manager.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.models.dto.BridgeDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.manager.BridgesService;
import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.api.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Processor;
import com.redhat.service.smartevents.processor.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.smartevents.processor.actions.sendtobridge.SendToBridgeAction;
import com.redhat.service.smartevents.processor.sources.slack.SlackSource;

import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class TestUtils {

    public static RequestSpecification jsonRequest() {
        return given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when();
    }

    public static Response getBridges() {
        return jsonRequest()
                .get(APIConstants.USER_API_BASE_PATH);
    }

    public static Response getBridgesFilterByName(String name) {
        return jsonRequest()
                .get(APIConstants.USER_API_BASE_PATH + "?name=" + name);
    }

    public static Response getBridgesFilterByStatus(ManagedResourceStatus... status) {
        String queryString = Arrays.stream(status).map(s -> "status=" + s.getValue()).collect(Collectors.joining("&"));
        return jsonRequest().get(APIConstants.USER_API_BASE_PATH + "?" + queryString);
    }

    public static Response getBridgesFilterByStatusWithAnyValue(String... status) {
        String queryString = Arrays.stream(status).map(s -> "status=" + s).collect(Collectors.joining("&"));
        return jsonRequest().get(APIConstants.USER_API_BASE_PATH + "?" + queryString);
    }

    public static Response getBridgesFilterByNameAndStatus(String name, ManagedResourceStatus... status) {
        String queryString = Arrays.stream(status).map(s -> "status=" + s.getValue()).collect(Collectors.joining("&"));
        return jsonRequest().get(APIConstants.USER_API_BASE_PATH + "?name=" + name + "&" + queryString);
    }

    public static Response getBridge(String id) {
        return jsonRequest()
                .get(APIConstants.USER_API_BASE_PATH + id);
    }

    public static Response listProcessors(String bridgeId, int page, int size) {
        return jsonRequest()
                .get(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors?size=" + size + "&page=" + page);
    }

    public static Response listProcessorsFilterByName(String bridgeId, String name) {
        return jsonRequest()
                .get(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors?name=" + name);
    }

    public static Response listProcessorsFilterByStatus(String bridgeId, ManagedResourceStatus... status) {
        String queryString = Arrays.stream(status).map(s -> "status=" + s.getValue()).collect(Collectors.joining("&"));
        return jsonRequest().get(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors?" + queryString);
    }

    public static Response listProcessorsFilterByStatusWithAnyValue(String bridgeId, String... status) {
        String queryString = Arrays.stream(status).map(s -> "status=" + s).collect(Collectors.joining("&"));
        return jsonRequest().get(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/?" + queryString);
    }

    public static Response listProcessorsFilterByType(String bridgeId, ProcessorType type) {
        return jsonRequest().get(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors?type=" + type.getValue());
    }

    public static Response listProcessorsFilterByTypeWithAnyValue(String bridgeId, String type) {
        return jsonRequest().get(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors?type=" + type);
    }

    public static Response listProcessorsFilterByNameAndStatus(String bridgeId, String name, ManagedResourceStatus... status) {
        String queryString = Arrays.stream(status).map(s -> "status=" + s.getValue()).collect(Collectors.joining("&"));
        return jsonRequest()
                .get(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors?name=" + name + "&" + queryString);
    }

    public static Response listProcessorsFilterByNameAndType(String bridgeId, String name, ProcessorType type) {
        return jsonRequest()
                .get(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors?name=" + name + "&type=" + type.getValue());
    }

    public static Response listProcessorsFilterByStatusAndType(String bridgeId, ManagedResourceStatus status, ProcessorType type) {
        return jsonRequest()
                .get(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors?status=" + status.getValue() + "&type=" + type.getValue());
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

    public static Response addProcessorToBridgeWithRequestBody(String bridgeId, String processorRequest) {
        return jsonRequest()
                .body(processorRequest)
                .post(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/");
    }

    public static Response deleteProcessor(String bridgeId, String processorId) {
        return jsonRequest()
                .delete(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/" + processorId);
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

    public static Response updateProcessor(String bridgeId, String processorId, ProcessorRequest p) {
        return jsonRequest()
                .body(p)
                .put(APIConstants.USER_API_BASE_PATH + bridgeId + "/processors/" + processorId);
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

    public static Response getProcessorsSchemaCatalog() {
        return jsonRequest()
                .get(APIConstants.SCHEMA_API_BASE_PATH);
    }

    public static Response getSourceProcessorsSchema(String name) {
        return jsonRequest()
                .get(APIConstants.SOURCES_SCHEMA_API_BASE_PATH + name);
    }

    public static Response getActionProcessorsSchema(String name) {
        return jsonRequest()
                .get(APIConstants.ACTIONS_SCHEMA_API_BASE_PATH + name);
    }

    public static Action createKafkaAction() {
        Action r = new Action();
        r.setType(KafkaTopicAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
        r.setParameters(params);
        return r;
    }

    public static Action createSendToBridgeAction(String bridgeId) {
        Action r = new Action();
        r.setType(SendToBridgeAction.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(SendToBridgeAction.BRIDGE_ID_PARAM, bridgeId);
        r.setParameters(params);
        return r;
    }

    public static Source createSlackSource() {
        Source s = new Source();
        s.setType(SlackSource.TYPE);

        Map<String, String> params = new HashMap<>();
        params.put(SlackSource.CHANNEL_PARAM, "channel");
        params.put(SlackSource.TOKEN_PARAM, "token");
        params.put(SlackSource.CLOUD_EVENT_TYPE, "ce");
        s.setParameters(params);
        return s;
    }

    public static Bridge waitForBridgeToBeReady(BridgesService bridgesService) {
        final List<Bridge> bridges = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridges.clear();
            bridges.addAll(bridgesService.findByShardIdWithReadyDependencies(TestConstants.SHARD_ID));
            assertThat(bridges.size()).isEqualTo(1);
        });
        return bridges.get(0);
    }

    public static Processor waitForProcessorDependenciesToBeReady(ProcessorDAO processorDAO, Processor processor) {
        final List<Processor> processors = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            processors.clear();
            Processor p = processorDAO.findById(processor.getId());
            assertThat(p).isNotNull();
            assertThat(p.getDependencyStatus()).isEqualTo(ManagedResourceStatus.READY);
            processors.add(p);
        });
        return processors.get(0);
    }

}
