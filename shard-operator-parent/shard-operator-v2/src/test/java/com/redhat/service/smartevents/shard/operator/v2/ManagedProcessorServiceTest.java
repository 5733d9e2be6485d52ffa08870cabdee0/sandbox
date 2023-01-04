package com.redhat.service.smartevents.shard.operator.v2;

import java.util.List;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.infra.v2.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.shard.operator.v2.resources.ManagedProcessor;
import com.redhat.service.smartevents.shard.operator.v2.utils.Fixtures;
import com.redhat.service.smartevents.shard.operator.v2.utils.V2KubernetesResourcePatcher;

import io.javaoperatorsdk.operator.Operator;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithOpenShiftTestServer;

@QuarkusTest
@WithOpenShiftTestServer
public class ManagedProcessorServiceTest {

    @Inject
    Operator operator;

    @Inject
    ManagedProcessorService managedProcessorService;

    @Inject
    V2KubernetesResourcePatcher kubernetesResourcePatcher;

    @BeforeEach
    public void beforeEach() {
        kubernetesResourcePatcher.cleanUp();
        operator.start();
    }

    @Test
    public void testCreateManagedProcessor() {
        // setup
        ProcessorDTO processorDTO = Fixtures.createProcessor(OperationType.CREATE);

        // test
        managedProcessorService.createManagedProcessor(processorDTO);

        // assert
        List<ManagedProcessor> managedProcessorList = managedProcessorService.fetchAllManagedProcessors();
        Assertions.assertThat(managedProcessorList.size()).isEqualTo(1);
    }

    @Test
    public void testDeleteManagedProcessor() {
        // setup
        ProcessorDTO processorDTO1 = Fixtures.createProcessor(OperationType.CREATE);
        managedProcessorService.createManagedProcessor(processorDTO1);

        List<ManagedProcessor> managedProcessorList = managedProcessorService.fetchAllManagedProcessors();
        Assertions.assertThat(managedProcessorList.size()).isEqualTo(1);

        // test
        managedProcessorService.deleteManagedProcessor(processorDTO1);

        // assert
        Assertions.assertThat(managedProcessorService.fetchAllManagedProcessors().size()).isEqualTo(0);
    }

    @Test
    public void testFetchAllManagedProcessors() {
        // setup
        ProcessorDTO processorDTO1 = Fixtures.createProcessor(OperationType.CREATE);
        processorDTO1.setId("1");
        managedProcessorService.createManagedProcessor(processorDTO1);

        ProcessorDTO processorDTO2 = Fixtures.createProcessor(OperationType.CREATE);
        processorDTO2.setId("2");
        managedProcessorService.createManagedProcessor(processorDTO2);

        // test
        List<ManagedProcessor> managedProcessorList = managedProcessorService.fetchAllManagedProcessors();

        // assert
        Assertions.assertThat(managedProcessorList.size()).isEqualTo(2);
    }
}
