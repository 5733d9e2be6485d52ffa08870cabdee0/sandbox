package com.redhat.service.smartevents.manager.workers.resources;

import java.util.stream.Stream;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.dao.BridgeDAO;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.Work;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import com.redhat.service.smartevents.manager.workers.WorkManager;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource(PostgresResource.class)
public class BridgeWorkerTest {

    private static final String TEST_RESOURCE_ID = "123";
    private static final String TEST_TOPIC_NAME = "TopicName";

    @InjectMock
    RhoasService rhoasService;

    @InjectMock
    ResourceNamesProvider resourceNamesProvider;

    @Inject
    WorkManager workManager;

    @Inject
    BridgeWorker worker;

    @Inject
    BridgeDAO bridgeDAO;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @BeforeEach
    public void setup() {
        databaseManagerUtils.cleanUp();
    }

    @Test
    void handleWorkProvisioningWithUnknownResource() {
        Work work = new Work();
        work.setManagedResourceId(TEST_RESOURCE_ID);

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("srcHandleWorkProvisioningWithKnownResource")
    void handleWorkProvisioningWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhosError,
            boolean isWorkComplete) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setStatus(status);
        bridgeDAO.persist(bridge);

        Work work = workManager.schedule(bridge);

        when(resourceNamesProvider.getBridgeTopicName(bridge.getId())).thenReturn(TEST_TOPIC_NAME);
        if (throwRhosError) {
            when(rhoasService.createTopicAndGrantAccessFor(any(), any())).thenThrow(new InternalPlatformException("error"));
        }

        Bridge refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        assertThat(workManager.exists(work)).isNotEqualTo(isWorkComplete);
        verify(rhoasService).createTopicAndGrantAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
    }

    private static Stream<Arguments> srcHandleWorkProvisioningWithKnownResource() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.READY, false, true),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.PROVISIONING, true, false),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.READY, false, true),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.PROVISIONING, true, false));
    }

    @Transactional
    @ParameterizedTest
    @MethodSource("srcHandleWorkDeletingWithKnownResource")
    void handleWorkDeletingWithKnownResource(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhosError,
            boolean isWorkComplete) {
        Bridge bridge = Fixtures.createBridge();
        bridge.setStatus(status);
        bridgeDAO.persist(bridge);

        Work work = workManager.schedule(bridge);

        when(resourceNamesProvider.getBridgeTopicName(bridge.getId())).thenReturn(TEST_TOPIC_NAME);
        if (throwRhosError) {
            doThrow(new InternalPlatformException("error")).when(rhoasService).deleteTopicAndRevokeAccessFor(any(), any());
        }

        Bridge refreshed = worker.handleWork(work);

        assertThat(refreshed.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        assertThat(workManager.exists(work)).isNotEqualTo(isWorkComplete);
        verify(rhoasService).deleteTopicAndRevokeAccessFor(TEST_TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
    }

    private static Stream<Arguments> srcHandleWorkDeletingWithKnownResource() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETED, false, true),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETING, true, false),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETED, false, true),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, true, false));
    }

}
