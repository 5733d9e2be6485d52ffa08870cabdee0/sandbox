package com.redhat.service.smartevents.shard.operator.core.resources;

import java.util.HashSet;

// used for testing purposes
public class FooResourceStatus extends CustomResourceStatus {

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
