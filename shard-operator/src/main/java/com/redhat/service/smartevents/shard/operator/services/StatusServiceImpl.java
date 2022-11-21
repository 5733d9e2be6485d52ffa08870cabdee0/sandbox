package com.redhat.service.smartevents.shard.operator.services;

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
    public void updateStatusForFailedReconciliation(CustomResourceStatus customResourceStatus, String conditionType, RuntimeException e) {
        Optional<Condition> conditionOpt =  customResourceStatus.getConditionByType(conditionType);
        updateCondition(conditionOpt.get(), e);
    }

    private void updateCondition(Condition condition, RuntimeException e) {
        if (e == null) {
            if (condition.getStatus() != ConditionStatus.True) {
                condition.setLastTransitionTime(new Date());
            }
            condition.setStatus(ConditionStatus.True);
            condition.setMessage(null);
        } else {
            condition.setStatus(ConditionStatus.False);
            condition.setMessage(e.getMessage());
            condition.setLastTransitionTime(new Date());
        }
    }
}
