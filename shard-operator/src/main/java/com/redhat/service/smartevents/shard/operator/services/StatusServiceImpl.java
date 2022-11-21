package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.exceptions.ReconcilationFailedException;
import com.redhat.service.smartevents.shard.operator.resources.Condition;
import com.redhat.service.smartevents.shard.operator.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.resources.CustomResourceStatus;

import javax.enterprise.context.ApplicationScoped;
import java.util.Date;
import java.util.Optional;

@ApplicationScoped
public class StatusServiceImpl implements StatusService {

    @Override
    public void updateStatusForSuccessfulReconciliation(CustomResourceStatus customResourceStatus, String conditionType) {
        Optional<Condition> conditionOpt =  customResourceStatus.getConditionByType(conditionType);
        updateCondition(conditionOpt.get(), null);
    }

    @Override
    public void updateStatusForFailedReconciliation(CustomResourceStatus customResourceStatus, ReconcilationFailedException e) {
        Optional<Condition> conditionOpt =  customResourceStatus.getConditionByType(e.getConditionType());
        updateCondition(conditionOpt.get(), e);
    }

    private void updateCondition(Condition condition, RuntimeException e) {
        if (e == null) {
            condition.setStatus(ConditionStatus.True);
            condition.setMessage("");
        } else {
            condition.setStatus(ConditionStatus.False);
            condition.setMessage(e.getMessage());
        }
        condition.setLastTransitionTime(new Date());
    }
}
