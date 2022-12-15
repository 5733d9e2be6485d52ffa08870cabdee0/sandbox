package com.redhat.service.smartevents.manager.v2.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;
import com.redhat.service.smartevents.manager.v2.TestConstants;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.persistence.models.ManagedResourceV2;
import com.redhat.service.smartevents.manager.v2.persistence.models.Processor;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.Fixtures;
import com.redhat.service.smartevents.manager.v2.utils.StatusUtilities;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.manager.v2.workers.resources.WorkerTestUtils.makeWork;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class ProcessorWorkerTest {

    private static final String TEST_RESOURCE_ID = "123";

    @V2
    @InjectMock
    WorkManager workManagerMock;

    @Inject
    ProcessorWorker worker;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    ProcessorDAO processorDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void setup() {
        databaseManagerUtils.cleanUp();
    }

    @Test
    void handleWorkProvisioningWithUnknownResource() {
        Work work = makeWork(TEST_RESOURCE_ID, 0, ZonedDateTime.now(ZoneOffset.UTC));

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Transactional
    void handleWorkProvisioningWithKnownResource() {
        Processor processor = createAndPersistDefaultAcceptedProcessor();

        Work work = makeWork(processor);

        worker.handleWork(work);

        Processor retrieved = processorDAO.findByIdWithConditions(processor.getId());

        assertThat(StatusUtilities.getManagedResourceStatus(retrieved)).isEqualTo(ManagedResourceStatus.PROVISIONING);
        verify(workManagerMock, never()).reschedule(any());
    }

    @Transactional
    protected Processor createAndPersistDefaultAcceptedProcessor() {
        Bridge bridge = Fixtures.createAcceptedBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        Processor processor = Fixtures.createAcceptedProcessor(bridge);
        processorDAO.persist(processor);
        return processor;
    }

    @Test
    @Transactional
    void handleWorkDeletingWithKnownResource() {
        Processor processor = createAndPersistDefaultDeprovisionProcessor();

        Work work = makeWork(processor);

        worker.handleWork(work);

        Processor retrieved = processorDAO.findByIdWithConditions(processor.getId());

        assertThat(StatusUtilities.getManagedResourceStatus(retrieved)).isEqualTo(ManagedResourceStatus.DELETING);
        verify(workManagerMock, never()).reschedule(any());
    }

    @Transactional
    protected Processor createAndPersistDefaultDeprovisionProcessor() {
        Bridge bridge = Fixtures.createAcceptedBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        bridgeDAO.persist(bridge);

        Processor processor = Fixtures.createDeprovisionProcessor(bridge);
        processorDAO.persist(processor);
        return processor;
    }

    @Test
    void testProcessorWorkerType() {
        Bridge bridge = Fixtures.createAcceptedBridge(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_BRIDGE_NAME);
        ManagedResourceV2 managedResourceV2 = Fixtures.createProcessor(bridge);
        Work work = Work.forResource(managedResourceV2);

        assertThat(worker.accept(work)).isTrue();
    }

}
