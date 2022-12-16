package com.redhat.service.smartevents.shard.operator.v2;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

@QuarkusTest
@WithOpenShiftTestServer
public class ManagerSyncServiceTest {

    @InjectMock
    ManagedBridgeSyncService managedBridgeSyncService;

    @InjectMock
    ManagedProcessorSyncService managedProcessorSyncService;
    @Inject
    ManagerSyncService managerSyncService;

    @Test
    public void TestSyncUpdatesFromManager() {
        // setup
        Mockito.doNothing().when(managedBridgeSyncService).syncManagedBridgeWithManager();
        Mockito.doNothing().when(managedProcessorSyncService).syncManagedProcessorWithManager();

        // test
        managerSyncService.syncUpdatesFromManager();

        // assert
        Mockito.verify(managedBridgeSyncService).syncManagedBridgeWithManager();
        Mockito.verify(managedProcessorSyncService).syncManagedProcessorWithManager();
    }
}
