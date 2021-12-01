package com.redhat.service.bridge.shard.operator.resources;

import java.util.HashSet;

// used for testing purposes
public class FooResourceStatus extends CustomResourceStatus {

    public FooResourceStatus() {
        super(new HashSet<Condition>() {
            {
                add(new Condition(ConditionType.Augmentation));
                add(new Condition(ConditionType.Ready));
            }
        });
    }
}
