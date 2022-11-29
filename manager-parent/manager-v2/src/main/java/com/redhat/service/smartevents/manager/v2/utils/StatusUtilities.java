package com.redhat.service.smartevents.manager.v2.utils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.redhat.service.smartevents.infra.core.models.ManagedResourceStatus;
import com.redhat.service.smartevents.infra.v2.api.models.ComponentType;
import com.redhat.service.smartevents.infra.v2.api.models.OperationType;
import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;
import com.redhat.service.smartevents.manager.v2.persistence.models.ManagedResourceV2;
import com.redhat.service.smartevents.manager.v2.persistence.models.Operation;

public class StatusUtilities {

    public final static String CONDITION_STATUS_TRUE = "True";
    public final static String CONDITION_STATUS_FALSE = "False";
    public final static String CONDITION_STATUS_UNKNOWN = "Unknown";
    public final static String CONDITION_STATUS_FAILED = "Failed";

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
        switch (resource.getOperation().getType()) {
            case CREATE:
            case UPDATE:
                if (Objects.isNull(conditions) || conditions.isEmpty()) {
                    return ManagedResourceStatus.ACCEPTED;
                }
                // The ordering of these checks is important!
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(CONDITION_STATUS_FAILED))) {
                    return ManagedResourceStatus.FAILED;
                }
                if (conditions.stream().allMatch(c -> c.getStatus().equals(CONDITION_STATUS_TRUE))) {
                    return ManagedResourceStatus.READY;
                }
                if (conditions.stream().filter(c -> c.getComponent() == ComponentType.MANAGER).allMatch(c -> c.getStatus().equals(CONDITION_STATUS_TRUE))) {
                    return ManagedResourceStatus.PROVISIONING;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(CONDITION_STATUS_TRUE))) {
                    return ManagedResourceStatus.PREPARING;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(CONDITION_STATUS_FALSE))) {
                    return ManagedResourceStatus.PREPARING;
                }
                if (conditions.stream().allMatch(c -> c.getStatus().equals(CONDITION_STATUS_UNKNOWN))) {
                    return ManagedResourceStatus.ACCEPTED;
                }
                break;
            case DELETE:
                if (Objects.isNull(conditions) || conditions.isEmpty()) {
                    return ManagedResourceStatus.DEPROVISION;
                }
                // Check "any matches" first as these have the widest scope
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(CONDITION_STATUS_FAILED))) {
                    return ManagedResourceStatus.FAILED;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(CONDITION_STATUS_TRUE))) {
                    return ManagedResourceStatus.DELETING;
                }
                if (conditions.stream().anyMatch(c -> c.getStatus().equals(CONDITION_STATUS_FALSE))) {
                    return ManagedResourceStatus.DELETING;
                }
                if (conditions.stream().allMatch(c -> c.getStatus().equals(CONDITION_STATUS_TRUE))) {
                    return ManagedResourceStatus.DELETED;
                }
                break;
        }
        return null;
    }

    public static String getStatusMessage(ManagedResourceV2 resource) {
        return resource.getConditions()
                .stream()
                .map(c -> "[" + c.getErrorCode() + "] " + c.getMessage())
                .collect(Collectors.joining(", "));
    }
}
