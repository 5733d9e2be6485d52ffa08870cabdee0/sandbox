package com.redhat.service.smartevents.shard.operator.resources;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.shard.operator.utils.DeploymentStatusUtils;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.internal.readiness.Readiness;

/**
 * Common interface for Kubernetes Custom Resource status
 */
public abstract class CustomResourceStatus {

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
    public void setConditionsFromDeployment(final Deployment d) {
        if (d.getStatus() == null) {
            this.markConditionFalse(ConditionTypeConstants.READY, ConditionReasonConstants.DEPLOYMENT_NOT_AVAILABLE, "");
            this.markConditionFalse(ConditionTypeConstants.AUGMENTATION);
        } else if (Readiness.isDeploymentReady(d)) {
            this.markConditionTrue(ConditionTypeConstants.READY, ConditionReasonConstants.DEPLOYMENT_AVAILABLE);
            this.markConditionFalse(ConditionTypeConstants.AUGMENTATION);
        } else {
            if (DeploymentStatusUtils.isTimeoutFailure(d)) {
                this.markConditionFalse(ConditionTypeConstants.READY, ConditionReasonConstants.DEPLOYMENT_FAILED, DeploymentStatusUtils.getReasonAndMessageForTimeoutFailure(d));
                this.markConditionFalse(ConditionTypeConstants.AUGMENTATION);
            } else if (DeploymentStatusUtils.isStatusReplicaFailure(d)) {
                this.markConditionFalse(ConditionTypeConstants.READY, ConditionReasonConstants.DEPLOYMENT_FAILED, DeploymentStatusUtils.getReasonAndMessageForReplicaFailure(d));
                this.markConditionFalse(ConditionTypeConstants.AUGMENTATION);
            } else {
                this.markConditionFalse(ConditionTypeConstants.READY, ConditionReasonConstants.DEPLOYMENT_NOT_AVAILABLE, "");
                this.markConditionTrue(ConditionTypeConstants.AUGMENTATION, ConditionReasonConstants.DEPLOYMENT_PROGRESSING);
            }
        }
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
}
