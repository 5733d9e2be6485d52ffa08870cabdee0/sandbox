package com.redhat.service.bridge.manager.api.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.actions.kafkatopic.KafkaTopicAction;
import com.redhat.service.bridge.actions.webhook.WebhookAction;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.infra.models.dto.KafkaConnectionDTO;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.filters.BaseFilter;
import com.redhat.service.bridge.infra.models.filters.StringEquals;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.WorkerSchedulerProfile;
import com.redhat.service.bridge.manager.actions.sendtobridge.SendToBridgeAction;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(WorkerSchedulerProfile.class)
public class ShardBridgesSyncAPITest {

    private static final String TEST_BRIDGE_ENDPOINT = "http://www.example.com/test-endpoint";
    private static final String TEST_BRIDGE_WEBHOOK = TEST_BRIDGE_ENDPOINT;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @InjectMock
    JsonWebToken jwt;

    @InjectMock
    RhoasService rhoasServiceMock;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
        when(jwt.getClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.SHARD_ID);
        when(jwt.containsClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessorsWithKafkaAction() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        //Emulate the Shard having deployed the Bridge
        BridgeDTO bridge =
                new BridgeDTO(bridgeResponse.getId(), bridgeResponse.getName(), TEST_BRIDGE_ENDPOINT, TestConstants.DEFAULT_CUSTOMER_ID, ManagedResourceStatus.READY, new KafkaConnectionDTO());
        TestUtils.updateBridge(bridge);

        //Create a Processor for the Bridge
        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(TestConstants.DEFAULT_PROCESSOR_NAME, filters, null, TestUtils.createKafkaAction()));

        final List<ProcessorDTO> processors = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            processors.clear();
            processors.addAll(TestUtils.getProcessorsToDeployOrDelete().as(new TypeRef<List<ProcessorDTO>>() {
            }));
            assertThat(processors.size()).isEqualTo(1);
        });

        ProcessorDTO processor = processors.get(0);
        assertThat(processor.getName()).isEqualTo(TestConstants.DEFAULT_PROCESSOR_NAME);
        assertThat(processor.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);
        assertThat(processor.getDefinition().getFilters().size()).isEqualTo(1);
        assertThat(processor.getDefinition().getRequestedAction()).isNotNull();
        assertThat(processor.getDefinition().getRequestedAction().getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(processor.getDefinition().getRequestedAction().getParameters()).containsEntry(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
        assertThat(processor.getDefinition().getResolvedAction()).isNotNull();
        assertThat(processor.getDefinition().getResolvedAction().getType()).isEqualTo(KafkaTopicAction.TYPE);
        assertThat(processor.getDefinition().getResolvedAction().getParameters()).containsEntry(KafkaTopicAction.TOPIC_PARAM, TestConstants.DEFAULT_KAFKA_TOPIC);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void getProcessorsWithSendToBridgeAction() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        String bridgeId = bridgeResponse.getId();
        //Emulate the Shard having deployed the Bridge
        BridgeDTO bridge = new BridgeDTO(bridgeId, bridgeResponse.getName(), TEST_BRIDGE_ENDPOINT, TestConstants.DEFAULT_CUSTOMER_ID, ManagedResourceStatus.READY, new KafkaConnectionDTO());
        TestUtils.updateBridge(bridge);

        //Create a Processor for the Bridge
        Set<BaseFilter> filters = Collections.singleton(new StringEquals("json.key", "value"));
        BaseAction action = TestUtils.createSendToBridgeAction(bridgeId);
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(TestConstants.DEFAULT_PROCESSOR_NAME, filters, null, action));

        final List<ProcessorDTO> processors = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            processors.clear();
            processors.addAll(TestUtils.getProcessorsToDeployOrDelete().as(new TypeRef<List<ProcessorDTO>>() {
            }));
            assertThat(processors.size()).isEqualTo(1);
        });

        ProcessorDTO processor = processors.get(0);
        assertThat(processor.getName()).isEqualTo(TestConstants.DEFAULT_PROCESSOR_NAME);
        assertThat(processor.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);
        assertThat(processor.getDefinition().getFilters().size()).isEqualTo(1);
        assertThat(processor.getDefinition().getRequestedAction()).isNotNull();
        assertThat(processor.getDefinition().getRequestedAction().getType()).isEqualTo(SendToBridgeAction.TYPE);
        assertThat(processor.getDefinition().getRequestedAction().getParameters()).containsEntry(SendToBridgeAction.BRIDGE_ID_PARAM, bridgeId);
        assertThat(processor.getDefinition().getResolvedAction()).isNotNull();
        assertThat(processor.getDefinition().getResolvedAction().getType()).isEqualTo(WebhookAction.TYPE);
        assertThat(processor.getDefinition().getResolvedAction().getParameters()).containsEntry(WebhookAction.ENDPOINT_PARAM, TEST_BRIDGE_WEBHOOK);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void updateProcessorStatus() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        BridgeDTO bridge =
                new BridgeDTO(bridgeResponse.getId(), bridgeResponse.getName(), TEST_BRIDGE_ENDPOINT, TestConstants.DEFAULT_CUSTOMER_ID, ManagedResourceStatus.READY, new KafkaConnectionDTO());
        TestUtils.updateBridge(bridge);
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(TestConstants.DEFAULT_PROCESSOR_NAME, TestUtils.createKafkaAction()));

        final List<ProcessorDTO> processors = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            processors.clear();
            processors.addAll(TestUtils.getProcessorsToDeployOrDelete().as(new TypeRef<List<ProcessorDTO>>() {
            }));
            assertThat(processors.size()).isEqualTo(1);
        });

        ProcessorDTO processor = processors.get(0);
        processor.setStatus(ManagedResourceStatus.READY);

        TestUtils.updateProcessor(processor);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            processors.clear();
            processors.addAll(TestUtils.getProcessorsToDeployOrDelete().as(new TypeRef<List<ProcessorDTO>>() {
            }));
            assertThat(processors).isEmpty();
        });
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void metricsAreProduced() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME)).as(BridgeResponse.class);
        BridgeDTO bridge =
                new BridgeDTO(bridgeResponse.getId(), bridgeResponse.getName(), TEST_BRIDGE_ENDPOINT, TestConstants.DEFAULT_CUSTOMER_ID, ManagedResourceStatus.READY, new KafkaConnectionDTO());
        TestUtils.updateBridge(bridge);
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(TestConstants.DEFAULT_PROCESSOR_NAME, TestUtils.createKafkaAction()));

        final List<ProcessorDTO> processors = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            processors.clear();
            processors.addAll(TestUtils.getProcessorsToDeployOrDelete().as(new TypeRef<List<ProcessorDTO>>() {
            }));
            assertThat(processors.size()).isEqualTo(1);
        });

        ProcessorDTO processor = processors.get(0);
        processor.setStatus(ManagedResourceStatus.READY);

        TestUtils.updateProcessor(processor);

        String metrics = given()
                .filter(new ResponseLoggingFilter())
                .contentType(ContentType.JSON)
                .when()
                .get("/q/metrics")
                .then()
                .extract()
                .body()
                .asString();
        assertThat(metrics).contains("manager_processor_status_change_total");
        assertThat(metrics).contains("manager_bridge_status_change_total");
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testGetEmptyBridgesToDeploy() {
        final List<BridgeDTO> bridgesToDeployOrDelete = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridgesToDeployOrDelete.clear();
            bridgesToDeployOrDelete.addAll(TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
            }));
            assertThat(bridgesToDeployOrDelete).isEmpty();
        });
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testGetBridgesToDeploy() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        final List<BridgeDTO> bridgesToDeployOrDelete = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridgesToDeployOrDelete.clear();
            bridgesToDeployOrDelete.addAll(TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
            }));
            assertThat(bridgesToDeployOrDelete.stream().filter(x -> x.getStatus().equals(ManagedResourceStatus.ACCEPTED)).count()).isEqualTo(1);
        });

        BridgeDTO bridge = bridgesToDeployOrDelete.get(0);
        assertThat(bridge.getName()).isEqualTo(TestConstants.DEFAULT_BRIDGE_NAME);
        assertThat(bridge.getCustomerId()).isEqualTo(TestConstants.DEFAULT_CUSTOMER_ID);
        assertThat(bridge.getStatus()).isEqualTo(ManagedResourceStatus.ACCEPTED);
        assertThat(bridge.getEndpoint()).isNull();
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testGetBridgesToDelete() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        final List<BridgeDTO> bridgesToDeployOrDelete = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridgesToDeployOrDelete.clear();
            bridgesToDeployOrDelete.addAll(TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
            }));
            assertThat(bridgesToDeployOrDelete.size()).isEqualTo(1);
        });

        BridgeDTO bridge = bridgesToDeployOrDelete.get(0);

        bridge.setStatus(ManagedResourceStatus.READY);
        TestUtils.updateBridge(bridge).then().statusCode(200);

        TestUtils.deleteBridge(bridge.getId()).then().statusCode(202);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridgesToDeployOrDelete.clear();
            bridgesToDeployOrDelete.addAll(TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
            }));

            assertThat(bridgesToDeployOrDelete.stream().filter(x -> x.getStatus().equals(ManagedResourceStatus.ACCEPTED)).count()).isZero();
            assertThat(bridgesToDeployOrDelete.stream().filter(x -> x.getStatus().equals(ManagedResourceStatus.DEPROVISION)).count()).isEqualTo(1);
        });

    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testNotifyDeployment() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        final List<BridgeDTO> bridgesToDeployOrDelete = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridgesToDeployOrDelete.clear();
            bridgesToDeployOrDelete.addAll(TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
            }));
            assertThat(bridgesToDeployOrDelete.stream().filter(x -> x.getStatus().equals(ManagedResourceStatus.ACCEPTED)).count()).isEqualTo(1);
        });

        BridgeDTO bridge = bridgesToDeployOrDelete.get(0);
        bridge.setStatus(ManagedResourceStatus.PROVISIONING);
        TestUtils.updateBridge(bridge).then().statusCode(200);

        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridgesToDeployOrDelete.clear();
            bridgesToDeployOrDelete.addAll(TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
            }));
            assertThat(bridgesToDeployOrDelete).isEmpty();
        });
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testNotifyDeletion() {
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        final List<BridgeDTO> bridgesToDeployOrDelete = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridgesToDeployOrDelete.clear();
            bridgesToDeployOrDelete.addAll(TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
            }));
            assertThat(bridgesToDeployOrDelete.size()).isEqualTo(1);
        });

        BridgeDTO bridge = bridgesToDeployOrDelete.get(0);

        bridge.setStatus(ManagedResourceStatus.READY);
        TestUtils.updateBridge(bridge).then().statusCode(200);

        TestUtils.deleteBridge(bridge.getId()).then().statusCode(202);

        BridgeResponse bridgeResponse = TestUtils.getBridge(bridge.getId()).as(BridgeResponse.class);
        assertThat(bridgeResponse.getStatus()).isEqualTo(ManagedResourceStatus.DEPROVISION);

        bridge.setStatus(ManagedResourceStatus.DELETED);
        TestUtils.updateBridge(bridge).then().statusCode(200);

        TestUtils.getBridge(bridge.getId()).then().statusCode(404);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void testUnauthorizedRole() {
        reset(jwt);
        when(jwt.getClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn("hacker");
        when(jwt.containsClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        TestUtils.getBridgesToDeployOrDelete().then().statusCode(403);
        TestUtils.getProcessorsToDeployOrDelete().then().statusCode(403);
        TestUtils.updateBridge(new BridgeDTO()).then().statusCode(403);
        TestUtils.updateProcessor(new ProcessorDTO()).then().statusCode(403);
    }
}
