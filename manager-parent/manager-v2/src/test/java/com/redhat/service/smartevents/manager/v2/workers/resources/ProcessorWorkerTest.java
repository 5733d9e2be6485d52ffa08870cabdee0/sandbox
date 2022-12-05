package com.redhat.service.smartevents.manager.v2.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.core.workers.WorkManager;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.dao.ProcessorDAO;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.manager.v2.workers.resources.WorkerTestUtils.makeWork;
import static org.assertj.core.api.Assertions.assertThatCode;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class ProcessorWorkerTest {

    private static final String TEST_RESOURCE_ID = "123";

    @InjectMock
    WorkManager workManager;

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
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();
    }

    @Test
    void handleWorkProvisioningWithUnknownResource() {
        Work work = makeWork(TEST_RESOURCE_ID, 0, ZonedDateTime.now(ZoneOffset.UTC));

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    //    @Transactional
    //    @ParameterizedTest
    //    @EnumSource(value = ManagedResourceStatus.class, names = { "ACCEPTED", "PREPARING" })
    //    void handleWorkProvisioningWithKnownResourceWithoutConnector(ManagedResourceStatus status) {
    //        Bridge bridge = Fixtures.createBridge();
    //        Processor processor = Fixtures.createProcessor(bridge, READY);
    ////        processor.setStatus(status);
    //        bridgeDAO.persist(bridge);
    //        processorDAO.persist(processor);
    //
    //        Work work = makeWork(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));
    //
    //        Processor refreshed = worker.handleWork(work);
    //
    ////        assertThat(refreshed.getDependencyStatus()).isEqualTo(READY);
    //        verify(workManager, never()).reschedule(any());
    //    }

    //    @Transactional
    //    @ParameterizedTest
    //    @EnumSource(value = ManagedResourceStatus.class, names = { "DEPROVISION", "DELETING" })
    //    void handleWorkDeletingWithKnownResourceWithoutConnector(ManagedResourceStatus status) {
    //        Bridge bridge = Fixtures.createBridge();
    //        Processor processor = Fixtures.createProcessor(bridge, READY);
    ////        processor.setStatus(status);
    //        bridgeDAO.persist(bridge);
    //        processorDAO.persist(processor);
    //
    //        Work work = makeWork(processor.getId(), 0, ZonedDateTime.now(ZoneOffset.UTC));
    //
    //        Processor refreshed = worker.handleWork(work);
    //
    //        assertThat(refreshed.getDependencyStatus()).isEqualTo(DELETED);
    //        verify(workManager, never()).reschedule(any());
    //    }
}
