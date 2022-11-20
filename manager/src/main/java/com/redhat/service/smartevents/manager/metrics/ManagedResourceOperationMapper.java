package com.redhat.service.smartevents.manager.metrics;

import com.redhat.service.smartevents.infra.metrics.MetricsOperation;
import com.redhat.service.smartevents.infra.models.dto.ManagedBridgeStatusUpdateDTO;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.manager.models.ManagedResource;

import java.util.Objects;

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
        UPDATE(MetricsOperation.MANAGER_RESOURCE_MODIFY),
        // The ManagedResource has been deleted
        DELETE(MetricsOperation.MANAGER_RESOURCE_DELETE),
        // The ManagedResource failed to be created
        FAILED_CREATE(MetricsOperation.MANAGER_RESOURCE_PROVISION),
        // The ManagedResource failed to be modified
        FAILED_UPDATE(MetricsOperation.MANAGER_RESOURCE_MODIFY),
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
     * and an update to be applied {@link ManagedBridgeStatusUpdateDTO}.
     * Only the "happy path" inference is handled; i.e. known transitions. All other
     * transitions result in {@see ManagedResourceOperation.NONE} being returned.
     * 
     * @param managedResource
     * @param updateDTO
     * @return
     */
    public static ManagedResourceOperation inferOperation(ManagedResource managedResource, ManagedBridgeStatusUpdateDTO updateDTO) {
        ManagedResourceStatus updateStatus = updateDTO.getStatus();
        ManagedResourceStatus resourceStatus = managedResource.getStatus();
        if (resourceStatus.equals(ManagedResourceStatus.DEPROVISION)
                || resourceStatus.equals(ManagedResourceStatus.DELETING)) {
            if (updateStatus.equals(ManagedResourceStatus.DELETED)) {
                return ManagedResourceOperation.DELETE;
            } else if (updateStatus.equals(ManagedResourceStatus.FAILED)) {
                return ManagedResourceOperation.FAILED_DELETE;
            }
        }
        if (resourceStatus.equals(ManagedResourceStatus.PREPARING)
                || resourceStatus.equals(ManagedResourceStatus.PROVISIONING)
                || resourceStatus.equals(ManagedResourceStatus.FAILED)) {
            if (updateStatus.equals(ManagedResourceStatus.READY)) {
                if (Objects.isNull(managedResource.getModifiedAt())) {
                    return ManagedResourceOperation.CREATE;
                } else {
                    return ManagedResourceOperation.UPDATE;
                }
            } else if (updateStatus.equals(ManagedResourceStatus.FAILED)) {
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
