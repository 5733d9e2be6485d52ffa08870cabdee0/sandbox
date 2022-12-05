package com.redhat.service.smartevents.manager.v2.workers.resources;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.core.services.RhoasService;
import com.redhat.service.smartevents.manager.core.workers.Work;
import com.redhat.service.smartevents.manager.v2.persistence.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.v2.persistence.models.Bridge;
import com.redhat.service.smartevents.manager.v2.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.v2.utils.Fixtures;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class AbstractWorkerTest {

    @InjectMock
    @SuppressWarnings("unused")
    //Needed to set up RHOAS for tests
    RhoasService rhoasService;

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
        Bridge bridge = Fixtures.createBridge();
        // Persist Bridge so that it can be found by the Worker
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge, maxRetries + 1);

        worker.handleWork(work);

        // TODO: check that it's failed
        //        assertThat(bridge.getStatus()).isEqualTo(ManagedResourceStatus.FAILED);
        //        assertThat(bridge.getErrorId()).isNotNull();
        //        assertThat(bridge.getErrorUUID()).isNotNull();
    }

    @Test
    @Transactional
    void workIsCompletedWhenTimedOut() {
        Bridge bridge = Fixtures.createBridge();
        // Persist Bridge so that it can be found by the Worker
        bridgeDAO.persist(bridge);

        Work work = WorkerTestUtils.makeWork(bridge, ZonedDateTime.now(ZoneOffset.UTC).minusSeconds(timeoutSeconds * 2L));

        worker.handleWork(work);

        // TODO: check that it's failed
        //        assertThat(bridge.getStatus()).isEqualTo(ManagedResourceStatus.FAILED);
        //        assertThat(bridge.getErrorId()).isNotNull();
        //        assertThat(bridge.getErrorUUID()).isNotNull();
    }

}
