package com.redhat.service.smartevents.shard.operator.services;

import com.redhat.service.smartevents.shard.operator.exceptions.DeltaProcessedException;
import com.redhat.service.smartevents.shard.operator.resources.Condition;
import com.redhat.service.smartevents.shard.operator.resources.ConditionStatus;
import com.redhat.service.smartevents.shard.operator.resources.CustomResourceStatus;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class StatusServiceImpl implements StatusService {

    @Override
    public void updateStatus(CustomResourceStatus customResourceStatus, String conditionType, RuntimeException e) {
        Optional<Condition> conditionOpt =  customResourceStatus.getConditionByType(conditionType);
        updateCondition(conditionOpt.get(), e);
    }

    private void updateCondition(Condition condition, RuntimeException e) {
        if (e != null) {
            if (e instanceof DeltaProcessedException) {
                condition.setStatus(ConditionStatus.True);
                condition.setMessage(e.getMessage());
            } else {
                condition.setStatus(ConditionStatus.False);
                condition.setMessage(e.getMessage());
            }
            throw e;
        }
    }
}
