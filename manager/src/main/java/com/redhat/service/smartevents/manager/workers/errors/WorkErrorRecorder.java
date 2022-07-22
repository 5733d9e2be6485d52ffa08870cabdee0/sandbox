package com.redhat.service.smartevents.manager.workers.errors;

import com.redhat.service.smartevents.infra.exceptions.BridgeError;

public interface WorkErrorRecorder {

    void deleteErrors(String managedResourceId);

    void recordError(String managedResourceId, Exception e);

    void recordError(String managedResourceId, BridgeError bridgeError);

}