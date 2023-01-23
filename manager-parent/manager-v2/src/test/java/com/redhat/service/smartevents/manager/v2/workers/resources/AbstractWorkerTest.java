package com.redhat.service.smartevents.manager.v2.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.manager.core.services.RhoasService;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.metrics.ManagerMetricsServiceV2;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.Fixtures;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class AbstractWorkerTest {

    @InjectMock
    @SuppressWarnings("unused")
    //Needed to set up RHOAS for tests
    RhoasService rhoasService;

    @InjectMock
    ManagerMetricsServiceV2 metricsService;

    @Inject
    BridgeWorker worker;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @ConfigProperty(name = "event-bridge.resources.worker.max-retries")
    int maxRetries;

    @ConfigProperty(name = "event-bridge.resources.workers.timeout-seconds")
    int timeoutSeconds;

    @BeforeEach
    public void setup() {
        databaseManagerUtils.cleanUp();
    }

    @Test
    @Transactional
    void workIsCompletedWhenMaxRetriesExceeded() {
        Bridge bridge = Fixtures.createAcceptedBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        // Persist Bridge so that it can be found by the Worker
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge, maxRetries + 1);

        worker.handleWork(work);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatusV2.FAILED);
    }

    @Test
    @Transactional
    void workIsCompletedWhenTimedOut() {
        Bridge bridge = Fixtures.createAcceptedBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        // Persist Bridge so that it can be found by the Worker
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge, ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(timeoutSeconds * 2L));

        worker.handleWork(work);

        assertThat(StatusUtilities.getManagedResourceStatus(bridge)).isEqualTo(ManagedResourceStatusV2.FAILED);
    }

    @Test
    @Transactional
    void metricsAreRecordedWhenWorkFails_Provisioning() {
        Bridge bridge = Fixtures.createAcceptedBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        // Persist Bridge so that it can be found by the Worker
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge, maxRetries + 1);

        worker.handleWork(work);

        verify(metricsService).onOperationFailed(eq(bridge), eq(MetricsOperation.MANAGER_RESOURCE_PROVISION));
    }

    @Test
    @Transactional
    void metricsAreRecordedWhenWorkFails_Updating() {
        Bridge bridge = Fixtures.createAcceptedBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        bridge.getOperation().setType(OperationType.UPDATE);
        // Persist Bridge so that it can be found by the Worker
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge, maxRetries + 1);

        worker.handleWork(work);

        verify(metricsService).onOperationFailed(eq(bridge), eq(MetricsOperation.MANAGER_RESOURCE_UPDATE));
    }

    @Test
    @Transactional
    void metricsAreRecordedWhenWorkFails_Deleting() {
        Bridge bridge = Fixtures.createDeprovisioningBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        // Persist Bridge so that it can be found by the Worker
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge, maxRetries + 1);

        worker.handleWork(work);

        verify(metricsService).onOperationFailed(eq(bridge), eq(MetricsOperation.MANAGER_RESOURCE_DELETE));
    }

}
