package com.redhat.service.smartevents.manager.v1.metrics;

import java.util.Objects;

import com.redhat.service.smartevents.infra.core.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.v1.api.dto.ManagedResourceStatusUpdateDTO;
import com.redhat.service.smartevents.infra.v1.api.models.ManagedResourceStatusV1;
import com.redhat.service.smartevents.manager.core.models.ManagedResource;
import com.redhat.service.smartevents.manager.v1.models.ManagedResourceV1;

public final class ManagedResourceOperationMapper {

    private ManagedResourceOperationMapper() {
        // Static utility functions only
    }

    public enum ManagedResourceOperation {

        // An inference was undetermined
        UNDETERMINED(null),
        // The ManagedResource has been created
        CREATE(MetricsOperation.MANAGER_RESOURCE_PROVISION),
        // The ManagedResource has been modified
        UPDATE(MetricsOperation.MANAGER_RESOURCE_UPDATE),
        // The ManagedResource has been deleted
        DELETE(MetricsOperation.MANAGER_RESOURCE_DELETE),
        // The ManagedResource failed to be created
        FAILED_CREATE(MetricsOperation.MANAGER_RESOURCE_PROVISION),
        // The ManagedResource failed to be modified
        FAILED_UPDATE(MetricsOperation.MANAGER_RESOURCE_UPDATE),
        // The ManagedResource failed to be deleted
        FAILED_DELETE(MetricsOperation.MANAGER_RESOURCE_DELETE);

        private final MetricsOperation metricsOperation;

        ManagedResourceOperation(MetricsOperation metricsOperation) {
            this.metricsOperation = metricsOperation;
        }

        public MetricsOperation getMetricsOperation() {
            return metricsOperation;
        }
    }

    /**
     * Infer an operation from the state of an existing {@link ManagedResource}
     * and an update to be applied {@link ManagedResourceStatusUpdateDTO}.
     * Only the "happy path" inference is handled; i.e. known transitions. All other
     * transitions result in {@see ManagedResourceOperation.NONE} being returned.
     * 
     * @param managedResource
     * @param updateDTO
     * @return
     */
    public static ManagedResourceOperation inferOperation(ManagedResourceV1 managedResource, ManagedResourceStatusUpdateDTO updateDTO) {
        ManagedResourceStatusV1 updateStatus = updateDTO.getStatus();
        ManagedResourceStatusV1 resourceStatus = managedResource.getStatus();
        if (resourceStatus.equals(ManagedResourceStatusV1.DEPROVISION)
                || resourceStatus.equals(ManagedResourceStatusV1.DELETING)) {
            if (updateStatus.equals(ManagedResourceStatusV1.DELETED)) {
                return ManagedResourceOperation.DELETE;
            } else if (updateStatus.equals(ManagedResourceStatusV1.FAILED)) {
                return ManagedResourceOperation.FAILED_DELETE;
            }
        }
        if (resourceStatus.equals(ManagedResourceStatusV1.PREPARING)
                || resourceStatus.equals(ManagedResourceStatusV1.PROVISIONING)
                || resourceStatus.equals(ManagedResourceStatusV1.FAILED)) {
            if (updateStatus.equals(ManagedResourceStatusV1.READY)) {
                if (Objects.isNull(managedResource.getModifiedAt())) {
                    return ManagedResourceOperation.CREATE;
                } else {
                    return ManagedResourceOperation.UPDATE;
                }
            } else if (updateStatus.equals(ManagedResourceStatusV1.FAILED)) {
                if (Objects.isNull(managedResource.getModifiedAt())) {
                    return ManagedResourceOperation.FAILED_CREATE;
                } else {
                    return ManagedResourceOperation.FAILED_UPDATE;
                }
            }
        }
        return ManagedResourceOperation.UNDETERMINED;
    }

}
