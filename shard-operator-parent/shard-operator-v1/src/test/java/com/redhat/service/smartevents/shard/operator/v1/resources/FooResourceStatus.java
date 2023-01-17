package com.redhat.service.smartevents.shard.operator.v1.resources;

import java.util.HashSet;

import com.redhat.service.smartevents.shard.operator.core.resources.Condition;

// used for testing purposes
public class FooResourceStatus extends BaseResourceStatus {

    public static final String AUGMENTATION = "Augmenting";

    public FooResourceStatus() {
        super(new HashSet<>() {
            {
                add(new Condition(ConditionTypeConstants.READY));
                add(new Condition(AUGMENTATION));
            }
        });
    }
}
