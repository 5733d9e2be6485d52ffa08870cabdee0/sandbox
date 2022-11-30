package com.redhat.service.smartevents.manager.v2.utils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.ConditionStatus;
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

    public static ManagedResourceStatus getManagedResourceStatus(ManagedResourceV2 resource) {
        List<Condition> conditions = resource.getConditions();
        if (Objects.isNull(conditions) || conditions.isEmpty()) {
            throw new IllegalStateException("Conditions can't be null or empty.");
        }
        if (conditions.stream().noneMatch(c -> c.getComponent() == ComponentType.MANAGER) || conditions.stream().noneMatch(c -> c.getComponent() == ComponentType.SHARD)) {
            throw new IllegalStateException("Conditions must contain at least one condition for the manager and one for the shard.");
        }

        switch (resource.getOperation().getType()) {
            case CREATE:
            case UPDATE:
                // The ordering of these checks is important!
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.FAILED))) {
                    return ManagedResourceStatus.FAILED;
                }
                if (conditions.stream().allMatch(c -> c.getStatus().equals(ConditionStatus.TRUE))) {
                    return ManagedResourceStatus.READY;
                }
                if (conditions.stream().filter(c -> c.getComponent() == ComponentType.MANAGER).allMatch(c -> c.getStatus().equals(ConditionStatus.TRUE))) {
                    return ManagedResourceStatus.PROVISIONING;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.TRUE))) {
                    return ManagedResourceStatus.PREPARING;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.FALSE))) {
                    return ManagedResourceStatus.PREPARING;
                }
                if (conditions.stream().allMatch(c -> c.getStatus().equals(ConditionStatus.UNKNOWN))) {
                    return ManagedResourceStatus.ACCEPTED;
                }
                break;
            case DELETE:
                // The ordering of these checks is important!
                if (conditions.stream().allMatch(c -> c.getStatus().equals(ConditionStatus.UNKNOWN))) {
                    return ManagedResourceStatus.DEPROVISION;
                }
                if (conditions.stream().allMatch(c -> c.getStatus().equals(ConditionStatus.TRUE))) {
                    return ManagedResourceStatus.DELETED;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.FAILED))) {
                    return ManagedResourceStatus.FAILED;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.TRUE))) {
                    return ManagedResourceStatus.DELETING;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(ConditionStatus.FALSE))) {
                    return ManagedResourceStatus.DELETING;
                }
                break;
        }
        return null;
    }

    public static String getStatusMessage(ManagedResourceV2 resource) {
        if (resource.getConditions() == null) {
            return null;
        }
        return resource.getConditions()
                .stream()
                .map(c -> "[" + c.getErrorCode() + "] " + c.getMessage())
                .collect(Collectors.joining(", "));
    }
}
