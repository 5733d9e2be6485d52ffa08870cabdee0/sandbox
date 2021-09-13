package com.redhat.developer.shard;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.redhat.developer.infra.dto.BridgeDTO;
import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.shard.controllers.ProcessorController;

import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;

import static org.mockito.Mockito.verify;

@QuarkusTest
public class OperatorServiceInMemoryImplTest {

    @Inject
    private OperatorServiceInMemoryImpl operator;

    private ProcessorController processorController;

    @BeforeEach
    public void before() {
        processorController = Mockito.mock(ProcessorController.class);
        QuarkusMock.installMockForType(processorController, ProcessorController.class);
    }

    @Test
    public void reconcileLoopMock_reconcileProcessorsWhenBridgeIsAvailable() {

        BridgeDTO b = new BridgeDTO("id", "name", "endpoint", "customerId", BridgeStatus.AVAILABLE);
        operator.createBridgeDeployment(b);
        operator.reconcileLoopMock();

        verify(processorController).reconcileProcessorsFor(b);
    }
}
