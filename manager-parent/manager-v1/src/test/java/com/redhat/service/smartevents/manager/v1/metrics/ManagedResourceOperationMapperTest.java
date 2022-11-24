package com.redhat.service.smartevents.manager.v1.metrics;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.infra.core.api.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.v1.metrics.ManagedResourceOperationMapper.ManagedResourceOperation;
import com.redhat.service.smartevents.manager.v1.models.ManagedResourceV1;

import static org.assertj.core.api.Assertions.assertThat;

public class ManagedResourceOperationMapperTest {

    @ParameterizedTest
    @MethodSource("inferenceTestData")
    void testInference(ManagedResourceStatus managedResourceStatus,
            boolean isManagedResourceModified,
            ManagedResourceStatus updateStatus,
            ManagedResourceOperation operation) {
        ManagedResourceV1 managedResource = new ManagedResourceV1();
        managedResource.setStatus(managedResourceStatus);
        managedResource.setModifiedAt(isManagedResourceModified ? ZonedDateTime.now() : null);

        ManagedResourceStatusUpdateDTO updateDTO = new ManagedResourceStatusUpdateDTO();
        updateDTO.setStatus(updateStatus);

        assertThat(ManagedResourceOperationMapper.inferOperation(managedResource, updateDTO)).isEqualTo(operation);
    }

    private static Stream<Arguments> inferenceTestData() {
        Object[][] arguments = {
                { ManagedResourceStatus.READY, false, ManagedResourceStatus.READY, ManagedResourceOperation.UNDETERMINED },
                { ManagedResourceStatus.PREPARING, false, ManagedResourceStatus.READY, ManagedResourceOperation.CREATE },
                { ManagedResourceStatus.PROVISIONING, false, ManagedResourceStatus.READY, ManagedResourceOperation.CREATE },
                { ManagedResourceStatus.FAILED, false, ManagedResourceStatus.READY, ManagedResourceOperation.CREATE },
                { ManagedResourceStatus.PREPARING, false, ManagedResourceStatus.FAILED, ManagedResourceOperation.FAILED_CREATE },
                { ManagedResourceStatus.PROVISIONING, false, ManagedResourceStatus.FAILED, ManagedResourceOperation.FAILED_CREATE },
                { ManagedResourceStatus.FAILED, false, ManagedResourceStatus.FAILED, ManagedResourceOperation.FAILED_CREATE },
                { ManagedResourceStatus.PREPARING, true, ManagedResourceStatus.READY, ManagedResourceOperation.UPDATE },
                { ManagedResourceStatus.PROVISIONING, true, ManagedResourceStatus.READY, ManagedResourceOperation.UPDATE },
                { ManagedResourceStatus.FAILED, true, ManagedResourceStatus.READY, ManagedResourceOperation.UPDATE },
                { ManagedResourceStatus.PREPARING, true, ManagedResourceStatus.FAILED, ManagedResourceOperation.FAILED_UPDATE },
                { ManagedResourceStatus.PROVISIONING, true, ManagedResourceStatus.FAILED, ManagedResourceOperation.FAILED_UPDATE },
                { ManagedResourceStatus.FAILED, true, ManagedResourceStatus.FAILED, ManagedResourceOperation.FAILED_UPDATE },
                { ManagedResourceStatus.DEPROVISION, false, ManagedResourceStatus.DELETED, ManagedResourceOperation.DELETE },
                { ManagedResourceStatus.DELETING, false, ManagedResourceStatus.DELETED, ManagedResourceOperation.DELETE },
                { ManagedResourceStatus.DEPROVISION, false, ManagedResourceStatus.FAILED, ManagedResourceOperation.FAILED_DELETE },
                { ManagedResourceStatus.DELETING, false, ManagedResourceStatus.FAILED, ManagedResourceOperation.FAILED_DELETE }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

}
