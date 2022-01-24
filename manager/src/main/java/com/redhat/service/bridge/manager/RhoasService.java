package com.redhat.service.bridge.manager;

import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

public interface RhoasService {

    boolean isEnabled();

    String createTopicAndGrantAccessForBridge(String bridgeId, RhoasTopicAccessType accessType);

    void deleteTopicAndRevokeAccessForBridge(String bridgeId, RhoasTopicAccessType accessType);

    String createTopicAndGrantAccessForProcessor(String processorId, RhoasTopicAccessType accessType);

    void deleteTopicAndRevokeAccessForProcessor(String processorId, RhoasTopicAccessType accessType);
}
