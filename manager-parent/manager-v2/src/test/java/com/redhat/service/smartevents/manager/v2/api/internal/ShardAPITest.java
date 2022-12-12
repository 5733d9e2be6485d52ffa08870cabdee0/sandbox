package com.redhat.service.smartevents.manager.v2.api.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.manager.core.services.RhoasService;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.requests.ProcessorRequest;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.BridgeResponse;
import com.redhat.service.smartevents.manager.v2.api.user.models.responses.ProcessorResponse;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.infra.core.api.APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.infra.core.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_BRIDGE_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CLOUD_PROVIDER;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_CUSTOMER_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_ORGANISATION_ID;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_PROCESSOR_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_REGION;
import static com.redhat.service.smartevents.manager.v2.TestConstants.DEFAULT_USER_NAME;
import static com.redhat.service.smartevents.manager.v2.TestConstants.SHARD_ID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ShardAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorDAO processorDAO;

    @InjectMock
    JsonWebToken jwt;

    @InjectMock
    @SuppressWarnings("unused")
    RhoasService rhoasServiceMock;

    @V2
    @InjectMock
    @SuppressWarnings("unused")
    // Prevent WorkManager from kicking in as we'll mock all activities
    WorkManager workManager;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
        when(jwt.getClaim(ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(SHARD_ID);
        when(jwt.containsClaim(ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(DEFAULT_ORGANISATION_ID);
        when(jwt.containsClaim(ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(DEFAULT_USER_NAME);
        when(jwt.containsClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(true);
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void getProcessors() {
        BridgeResponse bridgeResponse = TestUtils.createBridge(new BridgeRequest(DEFAULT_BRIDGE_NAME, DEFAULT_CLOUD_PROVIDER, DEFAULT_REGION)).as(BridgeResponse.class);

        //Emulate the Shard having deployed the Bridge
        mockBridgeCreation(bridgeResponse.getId());

        //Create two Processors for the Bridge
        ProcessorResponse processorResponse1 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME + "-1")).as(ProcessorResponse.class);
        TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest(DEFAULT_PROCESSOR_NAME + "-2")).as(ProcessorResponse.class);
        //Emulate the Processor1 dependencies being completed in the Manager and hence becoming visible to the Shard
        mockProcessorControlPlaneActivitiesComplete(processorResponse1.getId());

        final List<ProcessorDTO> processors = new ArrayList<>();
        await().atMost(5, SECONDS)
                .untilAsserted(() -> {
                    processors.clear();
                    processors.addAll(TestUtils.getProcessorsToDeployOrDelete().as(new TypeRef<List<ProcessorDTO>>() {
                    }));
                    assertThat(processors.size()).isEqualTo(1);
                });

        ProcessorDTO processor = processors.get(0);
        assertThat(processor.getName()).isEqualTo(processorResponse1.getName());
        assertThat(processor.getBridgeId()).isEqualTo(bridgeResponse.getId());
        assertThat(processor.getCustomerId()).isEqualTo(DEFAULT_CUSTOMER_ID);
        assertThat(processor.getOwner()).isEqualTo(DEFAULT_USER_NAME);
        assertThat(processor.getOperationType()).isEqualTo(OperationType.CREATE);
        assertThat(processor.getGeneration()).isEqualTo(0);
    }

    @Transactional
    protected void mockBridgeCreation(String bridgeId) {
        Bridge bridge = bridgeDAO.findByIdWithConditions(bridgeId);
        bridge.getConditions().forEach(c -> c.setStatus(ConditionStatus.TRUE));
    }

    @Transactional
    protected void mockProcessorControlPlaneActivitiesComplete(String processorId) {
        Processor processor = processorDAO.findByIdWithConditions(processorId);
        processor.getConditions().stream().filter(c -> c.getComponent() == ComponentType.MANAGER).forEach(c -> c.setStatus(ConditionStatus.TRUE));
    }

    @Test
    @TestSecurity(user = DEFAULT_CUSTOMER_ID)
    public void testUnauthorizedRole() {
        reset(jwt);
        when(jwt.getClaim(ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn("hacker");
        when(jwt.containsClaim(ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        TestUtils.getProcessorsToDeployOrDelete().then().statusCode(403);
    }
}
