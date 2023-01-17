package com.redhat.service.smartevents.manager.v2.utils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
import com.redhat.service.smartevents.infra.v2.api.models.ManagedResourceStatusV2;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.ManagedResourceV2;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;

public class StatusUtilities {

    private StatusUtilities() {
        //Static utility functions class
    }

    public static ZonedDateTime getModifiedAt(ManagedResourceV2 resource) {
        if (Objects.isNull(resource)) {
            return null;
        }
        Operation operation = resource.getOperation();
        if (Objects.nonNull(operation) && operation.getType() == OperationType.UPDATE) {
            return operation.getRequestedAt();
        }
        return null;
    }

    public static ManagedResourceStatusV2 getManagedResourceStatus(ManagedResourceV2 resource) {
        List<Condition> conditions = resource.getConditions();
        if (Objects.isNull(conditions) || conditions.isEmpty()) {
            throw new IllegalStateException("Conditions can't be null or empty.");
        }
        if (conditions.stream().noneMatch(c -> c.getComponent() == ComponentType.MANAGER) || conditions.stream().noneMatch(c -> c.getComponent() == ComponentType.SHARD)) {
            throw new IllegalStateException("Conditions must contain at least one condition for the manager and one for the shard.");
        }
        return getManagedResourceStatus(resource.getOperation(), conditions);
    }

    public static ManagedResourceStatusV2 getManagedResourceStatus(Operation operation, List<Condition> conditions) {
        switch (operation.getType()) {
            case CREATE:
                // The ordering of these checks is important!
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.FAILED))) {
                    return ManagedResourceStatusV2.FAILED;
                }
                if (conditions.stream().allMatch(c -> c.getStatus().equals(ConditionStatus.TRUE))) {
                    return ManagedResourceStatusV2.READY;
                }
                if (conditions.stream().filter(c -> c.getComponent() == ComponentType.MANAGER).allMatch(c -> c.getStatus().equals(ConditionStatus.TRUE))) {
                    return ManagedResourceStatusV2.PROVISIONING;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.TRUE))) {
                    return ManagedResourceStatusV2.PREPARING;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.FALSE))) {
                    return ManagedResourceStatusV2.PREPARING;
                }
                if (conditions.stream().allMatch(c -> c.getStatus().equals(ConditionStatus.UNKNOWN))) {
                    return ManagedResourceStatusV2.ACCEPTED;
                }
                break;
            case UPDATE:
                // The ordering of these checks is important!
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.FAILED))) {
                    return ManagedResourceStatusV2.FAILED;
                }
                if (conditions.stream().allMatch(c -> c.getStatus().equals(ConditionStatus.TRUE))) {
                    return ManagedResourceStatusV2.READY;
                }
                if (conditions.stream().filter(c -> c.getComponent() == ComponentType.MANAGER).allMatch(c -> c.getStatus().equals(ConditionStatus.TRUE))) {
                    return ManagedResourceStatusV2.UPDATE_PROVISIONING;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.TRUE))) {
                    return ManagedResourceStatusV2.UPDATE_PREPARING;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.FALSE))) {
                    return ManagedResourceStatusV2.UPDATE_PREPARING;
                }
                if (conditions.stream().allMatch(c -> c.getStatus().equals(ConditionStatus.UNKNOWN))) {
                    return ManagedResourceStatusV2.UPDATE_ACCEPTED;
                }
                break;
            case DELETE:
                // The ordering of these checks is important!
                if (conditions.stream().allMatch(c -> c.getStatus().equals(ConditionStatus.UNKNOWN))) {
                    return ManagedResourceStatusV2.DEPROVISION;
                }
                if (conditions.stream().allMatch(c -> c.getStatus().equals(ConditionStatus.TRUE))) {
                    return ManagedResourceStatusV2.DELETED;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.FAILED))) {
                    return ManagedResourceStatusV2.FAILED;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.TRUE))) {
                    return ManagedResourceStatusV2.DELETING;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.FALSE))) {
                    return ManagedResourceStatusV2.DELETING;
                }
                break;
        }
        return null;
    }

    public static boolean managerDependenciesCompleted(ManagedResourceV2 managedResourceV2) {
        if (Objects.isNull(managedResourceV2) || Objects.isNull(managedResourceV2.getConditions())) {
            return false;
        }
        return managedResourceV2.getConditions().stream().filter(c -> c.getComponent() == ComponentType.MANAGER).allMatch(c -> c.getStatus().equals(ConditionStatus.TRUE));
    }

    public static String getStatusMessage(ManagedResourceV2 resource) {
        if (Objects.isNull(resource) || Objects.isNull(resource.getConditions())) {
            return null;
        }
        List<Condition> errors = resource.getConditions()
                .stream()
                .filter(c -> c.getStatus() == ConditionStatus.FAILED)
                .filter(c -> Objects.nonNull(c.getErrorCode()))
                .collect(Collectors.toList());
        if (errors.isEmpty()) {
            return null;
        }
        return errors
                .stream()
                .map(c -> "[" + c.getErrorCode() + "]" + (Objects.nonNull(c.getMessage()) ? " " + c.getMessage() : ""))
                .collect(Collectors.joining(", "));
    }

    public static boolean isActionable(ManagedResourceV2 resource) {
        ManagedResourceStatusV2 status = getManagedResourceStatus(resource);
        return (status == ManagedResourceStatusV2.READY || status == ManagedResourceStatusV2.FAILED);
    }
}
