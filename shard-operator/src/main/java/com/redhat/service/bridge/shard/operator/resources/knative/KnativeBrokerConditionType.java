package com.redhat.service.bridge.shard.operator.resources.knative;

public enum KnativeBrokerConditionType {
    Addressable,
    ConfigMapUpdated,
    ConfigParsed,
    DataPlaneAvailable,
    ProbeSucceeded,
    Ready,
    TopicReady,
    BrokerReady,
    DeadLetterSinkResolved,
    DependencyReady,
    SubscriberResolved,
    SubscriptionReady
}
