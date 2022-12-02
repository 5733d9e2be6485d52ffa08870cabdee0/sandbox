package com.redhat.service.smartevents.shard.operator.v2;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class ManagerSyncServiceTest {

    @InjectMock
    ManagedBridgeSyncService managedBridgeSyncService;

    @Inject
    ManagerSyncService managerSyncService;

    @Test
    public void TestSyncUpdatesFromManager() {
        // setup
        Mockito.doNothing().when(managedBridgeSyncService).syncManagedBridgeWithManager();

        // test
        managerSyncService.syncUpdatesFromManager();

        // assert
        Mockito.verify(managedBridgeSyncService).syncManagedBridgeWithManager();
    }
}
