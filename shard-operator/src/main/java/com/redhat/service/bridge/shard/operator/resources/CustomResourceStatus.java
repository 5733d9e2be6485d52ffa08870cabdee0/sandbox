package com.redhat.service.bridge.shard.operator.resources;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.bridge.shard.operator.utils.DeploymentStatusUtils;

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
    public final Optional<Condition> getConditionByType(final ConditionType conditionType) {
        // o(1) operation since we are fetching by our hash key
        return conditions.stream().filter(c -> conditionType.equals(c.getType())).findFirst();
    }

    @JsonIgnore
    public final boolean isReady() {
        return conditions.stream().anyMatch(c -> ConditionType.Ready.equals(c.getType()) && ConditionStatus.True.equals(c.getStatus()));
    }

    @JsonIgnore
    public final boolean isConditionTypeTrue(final ConditionType conditionType) {
        return conditions.stream().anyMatch(c -> conditionType.equals(c.getType()) && ConditionStatus.True.equals(c.getStatus()));
    }

    @JsonIgnore
    public void setConditionsFromDeployment(final Deployment d) {
        if (d.getStatus() == null) {
            this.markConditionFalse(ConditionType.Ready, ConditionReason.DeploymentNotAvailable, "");
            this.markConditionFalse(ConditionType.Augmentation);
        } else if (Readiness.isDeploymentReady(d)) {
            this.markConditionTrue(ConditionType.Ready, ConditionReason.DeploymentAvailable);
            this.markConditionFalse(ConditionType.Augmentation);
        } else {
            if (DeploymentStatusUtils.isStatusReplicaFailure(d)) {
                this.markConditionFalse(ConditionType.Ready, ConditionReason.DeploymentFailed, DeploymentStatusUtils.getReasonAndMessageForReplicaFailure(d));
                this.markConditionFalse(ConditionType.Augmentation);
            } else {
                this.markConditionFalse(ConditionType.Ready, ConditionReason.DeploymentNotAvailable, "");
                this.markConditionTrue(ConditionType.Augmentation, ConditionReason.DeploymentProgressing);
            }
        }
    }

    public void markConditionFalse(final ConditionType conditionType, final ConditionReason reason, final String message, final String errorCode) {
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

    public void markConditionFalse(final ConditionType conditionType, final ConditionReason reason, final String message) {
        markConditionFalse(conditionType, reason, message, null);
    }

    public void markConditionFalse(final ConditionType conditionType) {
        markConditionFalse(conditionType, null, "", null);
    }

    public void markConditionTrue(final ConditionType conditionType, final ConditionReason reason) {
        final Optional<Condition> condition = this.getConditionByType(conditionType);
        if (condition.isPresent()) {
            condition.get().setMessage("");
            condition.get().setReason(reason);
            condition.get().setStatus(ConditionStatus.True);
            condition.get().setLastTransitionTime(new Date());
            this.conditions.add(condition.get());
        }
    }

    public void markConditionTrue(final ConditionType conditionType) {
        markConditionTrue(conditionType, null);
    }
}
