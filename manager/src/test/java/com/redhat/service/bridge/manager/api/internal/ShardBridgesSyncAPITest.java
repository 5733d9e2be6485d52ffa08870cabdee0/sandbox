package com.redhat.service.bridge.manager.api.internal;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

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
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ShardBridgesSyncAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessors() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        BridgeDTO bridge = new BridgeDTO(bridgeResponse.getId(), bridgeResponse.getName(), "myEndpoint", TestConstants.DEFAULT_CUSTOMER_ID, BridgeStatus.AVAILABLE);
        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));

        TestUtils.updateBridge(bridge);
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(TestConstants.DEFAULT_PROCESSOR_NAME, filters, null, TestUtils.createKafkaAction()));

        List<ProcessorDTO> processors = TestUtils.getProcessorsToDeployOrDelete().as(new TypeRef<List<ProcessorDTO>>() {
        });

        assertThat(processors.size()).isEqualTo(1);

        ProcessorDTO processor = processors.get(0);
        assertThat(processor.getName()).isEqualTo(TestConstants.DEFAULT_PROCESSOR_NAME);
        assertThat(processor.getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(processor.getFilters().size()).isEqualTo(1);
        assertThat(processor.getAction().getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(processor.getAction().getParameters()).containsEntry(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
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

        assertThat(processors.size()).isZero();
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testGetEmptyBridgesToDeploy() {
        List<BridgeDTO> response = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });
        assertThat(response.size()).isZero();
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testGetBridgesToDeploy() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        List<BridgeDTO> response = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });

        assertThat(response.stream().filter(x -> x.getStatus().equals(BridgeStatus.REQUESTED)).count()).isEqualTo(1);
        BridgeDTO bridge = response.get(0);
        assertThat(bridge.getName()).isEqualTo(TestConstants.DEFAULT_BRIDGE_NAME);
        assertThat(bridge.getCustomerId()).isEqualTo(TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(bridge.getStatus()).isEqualTo(BridgeStatus.REQUESTED);
        assertThat(bridge.getEndpoint()).isNull();
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testGetBridgesToDelete() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));
        List<BridgeDTO> bridgesToDeployOrDelete = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });
        BridgeDTO bridge = bridgesToDeployOrDelete.get(0);

        TestUtils.deleteBridge(bridge.getId()).then().statusCode(202);

        bridgesToDeployOrDelete = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });

        assertThat(bridgesToDeployOrDelete.stream().filter(x -> x.getStatus().equals(BridgeStatus.REQUESTED)).count()).isZero();
        assertThat(bridgesToDeployOrDelete.stream().filter(x -> x.getStatus().equals(BridgeStatus.DELETION_REQUESTED)).count()).isEqualTo(1);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testNotifyDeployment() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        List<BridgeDTO> bridgesToDeployOrDelete = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });

        assertThat(bridgesToDeployOrDelete.stream().filter(x -> x.getStatus().equals(BridgeStatus.REQUESTED)).count()).isEqualTo(1);

        BridgeDTO bridge = bridgesToDeployOrDelete.get(0);
        bridge.setStatus(BridgeStatus.PROVISIONING);
        TestUtils.updateBridge(bridge).then().statusCode(200);

        bridgesToDeployOrDelete = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });

        assertThat(bridgesToDeployOrDelete.size()).isZero();
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testNotifyDeletion() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        List<BridgeDTO> bridgesToDeploy = TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
        });
        BridgeDTO bridge = bridgesToDeploy.get(0);

        TestUtils.deleteBridge(bridge.getId()).then().statusCode(202);

        BridgeResponse bridgeResponse = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(bridgeResponse.getStatus()).isEqualTo(BridgeStatus.DELETION_REQUESTED);

        bridge.setStatus(BridgeStatus.DELETED);
        TestUtils.updateBridge(bridge).then().statusCode(200);

        TestUtils.getBridge(bridge.getId()).then().statusCode(404);
    }
}
