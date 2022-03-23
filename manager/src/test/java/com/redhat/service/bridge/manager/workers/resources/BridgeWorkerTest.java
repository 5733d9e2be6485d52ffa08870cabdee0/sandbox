package com.redhat.service.bridge.manager.workers.resources;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.bridge.infra.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.bridge.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.dao.BridgeDAO;
import com.redhat.service.bridge.manager.models.Bridge;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.bridge.manager.providers.ResourceNamesProvider;
import com.redhat.service.bridge.manager.workers.WorkManager;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BridgeWorkerTest {

    private static final String RESOURCE_ID = "123";

    private static final String TOPIC_NAME = "TopicName";

    @Mock
    BridgeDAO bridgeDAO;

    @Mock
    RhoasService rhoasService;

    @Mock
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;

    @Mock
    ResourceNamesProvider resourceNamesProvider;

    @Mock
    WorkManager workManager;

    @Mock
    EntityManager entityManager;

    @Captor
    ArgumentCaptor<Work> workArgumentCaptor;

    private BridgeWorker worker;

    @BeforeEach
    void setup() {
        this.worker = new BridgeWorker();
        this.worker.bridgeDAO = this.bridgeDAO;
        this.worker.rhoasService = this.rhoasService;
        this.worker.internalKafkaConfigurationProvider = this.internalKafkaConfigurationProvider;
        this.worker.resourceNamesProvider = this.resourceNamesProvider;
        this.worker.workManager = this.workManager;
        this.worker.maxRetries = 3;
        this.worker.timeoutSeconds = 60;
    }

    @Test
    void handleWorkProvisioningWithUnknownResource() {
        Work work = new Work();
        work.setManagedResourceId(RESOURCE_ID);

        when(bridgeDAO.findById(RESOURCE_ID)).thenReturn(null);

        assertThatCode(() -> worker.handleWork(work)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @MethodSource("srcHandleWorkProvisioningWithKnownResource")
    void handleWorkProvisioningWithKnownResourceWithConnector(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhosError,
            boolean isWorkComplete) {
        Work work = new Work();
        work.setManagedResourceId(RESOURCE_ID);
        work.setSubmittedAt(ZonedDateTime.now());

        Bridge bridge = new Bridge();
        bridge.setStatus(status);

        when(bridgeDAO.findById(RESOURCE_ID)).thenReturn(bridge);
        when(bridgeDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(bridge)).thenReturn(bridge);
        when(resourceNamesProvider.getBridgeTopicName(bridge.getId())).thenReturn(TOPIC_NAME);
        if (throwRhosError) {
            when(rhoasService.createTopicAndGrantAccessFor(any(), any())).thenThrow(new InternalPlatformException("error"));
        }

        worker.handleWork(work);

        assertThat(bridge.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(workManager, times(isWorkComplete ? 1 : 0)).complete(any(Work.class));
        verify(rhoasService).createTopicAndGrantAccessFor(TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
    }

    private static Stream<Arguments> srcHandleWorkProvisioningWithKnownResource() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.READY, false, true),
                Arguments.of(ManagedResourceStatus.ACCEPTED, ManagedResourceStatus.PROVISIONING, true, false),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.READY, false, true),
                Arguments.of(ManagedResourceStatus.PROVISIONING, ManagedResourceStatus.PROVISIONING, true, false));
    }

    @ParameterizedTest
    @MethodSource("srcHandleWorkDeletingWithKnownResource")
    void handleWorkDeletingWithKnownResource(ManagedResourceStatus status,
            ManagedResourceStatus dependencyStatusWhenComplete,
            boolean throwRhosError,
            boolean isWorkComplete) {
        Work work = new Work();
        work.setManagedResourceId(RESOURCE_ID);
        work.setSubmittedAt(ZonedDateTime.now());

        Bridge bridge = new Bridge();
        bridge.setStatus(status);

        when(bridgeDAO.findById(RESOURCE_ID)).thenReturn(bridge);
        when(bridgeDAO.getEntityManager()).thenReturn(entityManager);
        when(entityManager.merge(bridge)).thenReturn(bridge);
        when(resourceNamesProvider.getBridgeTopicName(bridge.getId())).thenReturn(TOPIC_NAME);
        if (throwRhosError) {
            doThrow(new InternalPlatformException("error")).when(rhoasService).deleteTopicAndRevokeAccessFor(any(), any());
        }

        worker.handleWork(work);

        assertThat(bridge.getDependencyStatus()).isEqualTo(dependencyStatusWhenComplete);
        verify(workManager, times(isWorkComplete ? 1 : 0)).complete(any(Work.class));
        verify(rhoasService).deleteTopicAndRevokeAccessFor(TOPIC_NAME, RhoasTopicAccessType.CONSUMER_AND_PRODUCER);
    }

    private static Stream<Arguments> srcHandleWorkDeletingWithKnownResource() {
        return Stream.of(
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETED, false, true),
                Arguments.of(ManagedResourceStatus.DEPROVISION, ManagedResourceStatus.DELETING, true, false),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETED, false, true),
                Arguments.of(ManagedResourceStatus.DELETING, ManagedResourceStatus.DELETING, true, false));
    }

}
