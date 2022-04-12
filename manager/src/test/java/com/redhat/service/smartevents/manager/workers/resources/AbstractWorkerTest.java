package com.redhat.service.smartevents.manager.workers.resources;

import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Work;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.manager.workers.WorkManager;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class AbstractWorkerTest {

    @InjectMock
    @SuppressWarnings("unused")
    //Needed to set up RHOAS for tests
    RhoasService rhoasService;

    @Inject
    WorkManager workManager;

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
    void workIsCompletedWhenResourceIsNotFound() {
        Bridge bridge = Fixtures.createBridge();
        // It is intentional that we do not persist the Bridge here
        Work work = workManager.schedule(bridge);
        assertThat(workManager.exists(work)).isTrue();

        assertThatThrownBy(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Transactional
    void workIsCompletedWhenMaxRetriedExceeded() {
        Bridge bridge = Fixtures.createBridge();
        // Persist Bridge so that it can be found by the Worker
        bridgeDAO.persist(bridge);

        Work work = workManager.schedule(bridge);
        assertThat(workManager.exists(work)).isTrue();
        work.setAttempts(maxRetries + 1);

        worker.handleWork(work);

        assertThat(bridge.getStatus()).isEqualTo(ManagedResourceStatus.FAILED);

        assertThat(workManager.exists(work)).isFalse();
    }

    @Test
    @Transactional
    void workIsCompletedWhenTimedOut() {
        Bridge bridge = Fixtures.createBridge();
        // Persist Bridge so that it can be found by the Worker
        bridgeDAO.persist(bridge);

        Work work = workManager.schedule(bridge);
        assertThat(workManager.exists(work)).isTrue();
        work.setSubmittedAt(ZonedDateTime.now().minusSeconds(timeoutSeconds * 2L));

        worker.handleWork(work);

        assertThat(bridge.getStatus()).isEqualTo(ManagedResourceStatus.FAILED);

        assertThat(workManager.exists(work)).isFalse();
    }

}
