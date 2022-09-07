package com.redhat.service.smartevents.shard.operator.resources;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.exceptions.BridgeError;
import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;
import com.redhat.service.smartevents.shard.operator.utils.DeploymentStatusUtils;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;

/**
 * Common interface for Kubernetes Custom Resource status
 */
public abstract class CustomResourceStatus extends ObservedGenerationAwareStatus {

    private final Set<Condition> conditions;

    @JsonCreator
    public CustomResourceStatus(@JsonProperty("conditions") final HashSet<Condition> initialConditions) {
        if (initialConditions == null) {
            throw new IllegalArgumentException("initialConditions can't be null");
        }
        this.conditions = initialConditions;
    }

    public final Set<Condition> getConditions() {
        return Collections.unmodifiableSet(this.conditions);
    }

    @JsonIgnore
    public final Optional<Condition> getConditionByType(final String conditionType) {
        // o(1) operation since we are fetching by our hash key
        return conditions.stream().filter(c -> conditionType.equals(c.getType())).findFirst();
    }

    @JsonIgnore
    public final boolean isReady() {
        return conditions.stream().anyMatch(c -> ConditionTypeConstants.READY.equals(c.getType()) && ConditionStatus.True.equals(c.getStatus()));
    }

    @JsonIgnore
    public final boolean isConditionTypeTrue(final String conditionType) {
        return conditions.stream().anyMatch(c -> conditionType.equals(c.getType()) && ConditionStatus.True.equals(c.getStatus()));
    }

    @JsonIgnore
    public final boolean isConditionTypeFalse(final String conditionType) {
        return conditions.stream().anyMatch(c -> conditionType.equals(c.getType()) && ConditionStatus.False.equals(c.getStatus()));
    }

    public void markConditionFalse(final String conditionType, final String reason, final String message, final String errorCode) {
        final Optional<Condition> condition = this.getConditionByType(conditionType);
        if (condition.isPresent()) {
            condition.get().setMessage(message);
            condition.get().setErrorCode(errorCode);
            condition.get().setReason(reason);
            condition.get().setStatus(ConditionStatus.False);
            condition.get().setLastTransitionTime(new Date());
            this.conditions.add(condition.get());
        }
    }

    public void markConditionFalse(final String conditionType, final String reason, final String message) {
        markConditionFalse(conditionType, reason, message, null);
    }

    public void markConditionFalse(final String conditionType) {
        markConditionFalse(conditionType, null, "", null);
    }

    public void markConditionTrue(final String conditionType, final String reason) {
        final Optional<Condition> condition = this.getConditionByType(conditionType);
        if (condition.isPresent()) {
            condition.get().setMessage("");
            condition.get().setReason(reason);
            condition.get().setStatus(ConditionStatus.True);
            condition.get().setLastTransitionTime(new Date());
            this.conditions.add(condition.get());
        }
    }

    public void markConditionTrue(final String conditionType) {
        markConditionTrue(conditionType, null);
    }

    @JsonIgnore
    public final void setStatusFromBridgeError(BridgeError bridgeError) {
        markConditionFalse(ConditionTypeConstants.READY,
                bridgeError.getReason(),
                bridgeError.getReason(),
                bridgeError.getCode());
        markConditionFalse(ConditionTypeConstants.AUGMENTATION);
    }

    @JsonIgnore
    public final void setStatusFromDeployment(Deployment deployment) {
        if (deployment.getStatus() == null) {
            markConditionFalse(ConditionTypeConstants.READY,
                    ConditionReasonConstants.DEPLOYMENT_NOT_AVAILABLE, "");
            markConditionTrue(ConditionTypeConstants.AUGMENTATION,
                    ConditionReasonConstants.DEPLOYMENT_PROGRESSING);
        } else if (Readiness.isDeploymentReady(deployment)) {
            markConditionFalse(ConditionTypeConstants.READY,
                    ConditionReasonConstants.DEPLOYMENT_AVAILABLE, "");
            markConditionTrue(ConditionTypeConstants.AUGMENTATION,
                    ConditionReasonConstants.DEPLOYMENT_AVAILABLE);
        } else {
            if (DeploymentStatusUtils.isTimeoutFailure(deployment)) {
                markConditionFalse(ConditionTypeConstants.READY,
                        ConditionReasonConstants.DEPLOYMENT_FAILED,
                        DeploymentStatusUtils.getReasonAndMessageForTimeoutFailure(deployment));
                markConditionFalse(ConditionTypeConstants.AUGMENTATION,
                        ConditionReasonConstants.DEPLOYMENT_FAILED, "");
            } else if (DeploymentStatusUtils.isStatusReplicaFailure(deployment)) {
                markConditionFalse(ConditionTypeConstants.READY,
                        ConditionReasonConstants.DEPLOYMENT_FAILED,
                        DeploymentStatusUtils.getReasonAndMessageForReplicaFailure(deployment));
                markConditionFalse(ConditionTypeConstants.AUGMENTATION,
                        ConditionReasonConstants.DEPLOYMENT_FAILED, "");
            } else {
                markConditionFalse(ConditionTypeConstants.READY,
                        ConditionReasonConstants.DEPLOYMENT_NOT_AVAILABLE, "");
                markConditionTrue(ConditionTypeConstants.AUGMENTATION,
                        ConditionReasonConstants.DEPLOYMENT_PROGRESSING);
            }
        }
    }

    public final ManagedResourceStatus inferManagedResourceStatus() {
        if (isReady()) {
            return ManagedResourceStatus.READY;
        }
        if (isConditionTypeFalse(ConditionTypeConstants.READY) && isConditionTypeFalse(ConditionTypeConstants.AUGMENTATION)) {
            return ManagedResourceStatus.FAILED;
        }
        return ManagedResourceStatus.PROVISIONING;
    }

}
