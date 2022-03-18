package com.redhat.service.bridge.manager.workers.resources;

import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.workers.WorkManager;
import com.redhat.service.bridge.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@QuarkusTest
@ExtendWith(MockitoExtension.class)
@QuarkusTestResource(PostgresResource.class)
public class AbstractWorkerTest {

    private static final String RESOURCE_ID = "123";

    @InjectMock
    WorkManager workManager;

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
    void workIsCompletedWhenResourceIsNotFound() {
        Work work = new Work();
        work.setType(Bridge.class.getName());
        work.setSubmittedAt(ZonedDateTime.now());
        work.setManagedResourceId(RESOURCE_ID);

        assertThatThrownBy(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);

        verify(workManager).complete(work);
    }

    @Test
    @Transactional
    void workIsCompletedWhenMaxRetriedExceeded() {
        Bridge bridge = new Bridge(TestConstants.DEFAULT_BRIDGE_NAME);
        bridge.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        bridge.setStatus(ManagedResourceStatus.ACCEPTED);
        bridge.setSubmittedAt(ZonedDateTime.now());
        bridge.setId(RESOURCE_ID);
        bridgeDAO.persist(bridge);

        Work work = new Work();
        work.setType(Bridge.class.getName());
        work.setSubmittedAt(bridge.getSubmittedAt());
        work.setManagedResourceId(RESOURCE_ID);
        work.setAttempts(maxRetries + 1);

        worker.handleWork(work);

        assertThat(bridge.getStatus()).isEqualTo(ManagedResourceStatus.FAILED);

        verify(workManager).complete(work);
    }

    @Test
    @Transactional
    void workIsCompletedWhenTimedOut() {
        Bridge bridge = new Bridge(TestConstants.DEFAULT_BRIDGE_NAME);
        bridge.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        bridge.setStatus(ManagedResourceStatus.ACCEPTED);
        bridge.setSubmittedAt(ZonedDateTime.now().minusSeconds(timeoutSeconds * 2L));
        bridge.setId(RESOURCE_ID);
        bridgeDAO.persist(bridge);

        Work work = new Work();
        work.setType(Bridge.class.getName());
        work.setSubmittedAt(bridge.getSubmittedAt());
        work.setManagedResourceId(RESOURCE_ID);

        worker.handleWork(work);

        assertThat(bridge.getStatus()).isEqualTo(ManagedResourceStatus.FAILED);

        verify(workManager).complete(work);
    }

}
