package com.redhat.service.bridge.executor;

import java.util.Set;

public interface ExecutorsProvider {

    /**
     * TODO: Implement and keep only this method (remove `getExecutors(String bridgeId)`), because when we will move to
     * k8s the application will consume only its events from its kafka topic.
     */
    Set<Executor> getExecutors();

    /**
     * TODO: Remove when we move to k8s
     */
    Set<Executor> getExecutors(String bridgeId);
}
