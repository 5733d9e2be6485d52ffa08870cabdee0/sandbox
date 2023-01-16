package com.redhat.service.smartevents.manager.v1.metrics;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.infra.v1.api.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1;
import com.redhat.service.smartevents.manager.v1.metrics.ManagedResourceOperationMapper.ManagedResourceOperation;
import com.redhat.service.smartevents.manager.v1.models.ManagedResourceV1;
import com.redhat.service.smartevents.manager.v1.persistence.models.Bridge;

import static org.assertj.core.api.Assertions.assertThat;

public class ManagedResourceOperationMapperTest {

    @ParameterizedTest
    @MethodSource("inferenceTestData")
    void testInference(ManagedResourceStatusV1 managedResourceStatus,
            boolean isManagedResourceModified,
            ManagedResourceStatusV1 updateStatus,
            ManagedResourceOperation operation) {
        ManagedResourceV1 managedResource = new Bridge();
        managedResource.setStatus(managedResourceStatus);
        managedResource.setModifiedAt(isManagedResourceModified ? ZonedDateTime.now() : null);

        ManagedResourceStatusUpdateDTO updateDTO = new ManagedResourceStatusUpdateDTO();
        updateDTO.setStatus(updateStatus);

        assertThat(ManagedResourceOperationMapper.inferOperation(managedResource, updateDTO)).isEqualTo(operation);
    }

    private static Stream<Arguments> inferenceTestData() {
        Object[][] arguments = {
                { ManagedResourceStatusV1.READY, false, ManagedResourceStatusV1.READY, ManagedResourceOperation.UNDETERMINED },
                { ManagedResourceStatusV1.PREPARING, false, ManagedResourceStatusV1.READY, ManagedResourceOperation.CREATE },
                { ManagedResourceStatusV1.PROVISIONING, false, ManagedResourceStatusV1.READY, ManagedResourceOperation.CREATE },
                { ManagedResourceStatusV1.FAILED, false, ManagedResourceStatusV1.READY, ManagedResourceOperation.CREATE },
                { ManagedResourceStatusV1.PREPARING, false, ManagedResourceStatusV1.FAILED, ManagedResourceOperation.FAILED_CREATE },
                { ManagedResourceStatusV1.PROVISIONING, false, ManagedResourceStatusV1.FAILED, ManagedResourceOperation.FAILED_CREATE },
                { ManagedResourceStatusV1.FAILED, false, ManagedResourceStatusV1.FAILED, ManagedResourceOperation.FAILED_CREATE },
                { ManagedResourceStatusV1.PREPARING, true, ManagedResourceStatusV1.READY, ManagedResourceOperation.UPDATE },
                { ManagedResourceStatusV1.PROVISIONING, true, ManagedResourceStatusV1.READY, ManagedResourceOperation.UPDATE },
                { ManagedResourceStatusV1.FAILED, true, ManagedResourceStatusV1.READY, ManagedResourceOperation.UPDATE },
                { ManagedResourceStatusV1.PREPARING, true, ManagedResourceStatusV1.FAILED, ManagedResourceOperation.FAILED_UPDATE },
                { ManagedResourceStatusV1.PROVISIONING, true, ManagedResourceStatusV1.FAILED, ManagedResourceOperation.FAILED_UPDATE },
                { ManagedResourceStatusV1.FAILED, true, ManagedResourceStatusV1.FAILED, ManagedResourceOperation.FAILED_UPDATE },
                { ManagedResourceStatusV1.DEPROVISION, false, ManagedResourceStatusV1.DELETED, ManagedResourceOperation.DELETE },
                { ManagedResourceStatusV1.DELETING, false, ManagedResourceStatusV1.DELETED, ManagedResourceOperation.DELETE },
                { ManagedResourceStatusV1.DEPROVISION, false, ManagedResourceStatusV1.FAILED, ManagedResourceOperation.FAILED_DELETE },
                { ManagedResourceStatusV1.DELETING, false, ManagedResourceStatusV1.FAILED, ManagedResourceOperation.FAILED_DELETE }
        };
        return Stream.of(arguments).map(Arguments::of);
    }

}
