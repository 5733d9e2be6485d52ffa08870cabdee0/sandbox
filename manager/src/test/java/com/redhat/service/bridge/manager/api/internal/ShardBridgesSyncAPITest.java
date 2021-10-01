package com.redhat.service.bridge.manager.api.internal;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringEquals;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;

@QuarkusTest
public class ShardBridgesSyncAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void getProcessors() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        BridgeDTO bridge = new BridgeDTO(bridgeResponse.getId(), bridgeResponse.getName(), "myEndpoint", TestConstants.DEFAULT_CUSTOMER_ID, BridgeStatus.AVAILABLE);
        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));

        TestUtils.updateBridge(bridge);
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(TestConstants.DEFAULT_PROCESSOR_NAME, filters, null, TestUtils.createKafkaAction()));

        List<ProcessorDTO> processors = TestUtils.getProcessorsToDeployOrDelete().as(new TypeRef<List<ProcessorDTO>>() {
        });

        Assertions.assertEquals(1, processors.size());

        ProcessorDTO processor = processors.get(0);
        Assertions.assertEquals(TestConstants.DEFAULT_PROCESSOR_NAME, processor.getName());
        Assertions.assertEquals(BridgeStatus.REQUESTED, processor.getStatus());
        Assertions.assertEquals(1, processor.getFilters().size());
        Assertions.assertEquals(KafkaTopicAction.TYPE, processor.getAction().getType());
        Assertions.assertEquals(TestConstants.DEFAULT_KAFKA_TOPIC, processor.getAction().getParameters().get(KafkaTopicAction.TOPIC_PARAM));
    }

    @Test
    public void updateProcessorStatus() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        BridgeDTO bridge = new BridgeDTO(bridgeResponse.getId(), bridgeResponse.getName(), "myEndpoint", TestConstants.DEFAULT_CUSTOMER_ID, BridgeStatus.AVAILABLE);
        TestUtils.updateBridge(bridge);
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(TestConstants.DEFAULT_PROCESSOR_NAME, TestUtils.createKafkaAction()));

        List<ProcessorDTO> processors = TestUtils.getProcessorsToDeployOrDelete().as(new TypeRef<List<ProcessorDTO>>() {
        });

        ProcessorDTO processor = processors.get(0);
        processor.setStatus(BridgeStatus.AVAILABLE);

        TestUtils.updateProcessor(processor);

        processors = TestUtils.getProcessorsToDeployOrDelete().as(new TypeRef<List<ProcessorDTO>>() {
        });

        Assertions.assertEquals(0, processors.size());
    }

    @Test
    public void testGetEmptyBridgesToDeploy() {
        List<BridgeDTO> response = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });
        Assertions.assertEquals(0, response.size());
    }

    @Test
    public void testGetBridgesToDeploy() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        List<BridgeDTO> response = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });

        Assertions.assertEquals(1, response.stream().filter(x -> x.getStatus().equals(BridgeStatus.REQUESTED)).count());
        BridgeDTO bridge = response.get(0);
        Assertions.assertEquals(TestConstants.DEFAULT_BRIDGE_NAME, bridge.getName());
        Assertions.assertEquals(TestConstants.DEFAULT_CUSTOMER_ID, bridge.getCustomerId());
        Assertions.assertEquals(BridgeStatus.REQUESTED, bridge.getStatus());
        Assertions.assertNull(bridge.getEndpoint());
    }

    @Test
    public void testGetBridgesToDelete() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));
        List<BridgeDTO> bridgesToDeployOrDelete = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });
        BridgeDTO bridge = bridgesToDeployOrDelete.get(0);

        TestUtils.deleteBridge(bridge.getId()).then().statusCode(202);

        bridgesToDeployOrDelete = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });

        Assertions.assertEquals(0, bridgesToDeployOrDelete.stream().filter(x -> x.getStatus().equals(BridgeStatus.REQUESTED)).count());
        Assertions.assertEquals(1, bridgesToDeployOrDelete.stream().filter(x -> x.getStatus().equals(BridgeStatus.DELETION_REQUESTED)).count());
    }

    @Test
    public void testNotifyDeployment() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        List<BridgeDTO> bridgesToDeployOrDelete = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });

        Assertions.assertEquals(1, bridgesToDeployOrDelete.stream().filter(x -> x.getStatus().equals(BridgeStatus.REQUESTED)).count());

        BridgeDTO bridge = bridgesToDeployOrDelete.get(0);
        bridge.setStatus(BridgeStatus.PROVISIONING);
        TestUtils.updateBridge(bridge).then().statusCode(200);

        bridgesToDeployOrDelete = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });

        Assertions.assertEquals(0, bridgesToDeployOrDelete.size());
    }

    @Test
    public void testNotifyDeletion() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        List<BridgeDTO> bridgesToDeploy = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });
        BridgeDTO bridge = bridgesToDeploy.get(0);

        TestUtils.deleteBridge(bridge.getId()).then().statusCode(202);

        BridgeResponse bridgeResponse = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        Assertions.assertEquals(BridgeStatus.DELETION_REQUESTED, bridgeResponse.getStatus());

        bridge.setStatus(BridgeStatus.DELETED);
        TestUtils.updateBridge(bridge).then().statusCode(200);

        TestUtils.getBridge(bridge.getId()).then().statusCode(404);
    }
}
