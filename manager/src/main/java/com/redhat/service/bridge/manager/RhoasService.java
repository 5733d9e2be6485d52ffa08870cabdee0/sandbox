package com.redhat.service.bridge.manager;

import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

public interface RhoasService {

    boolean isEnabled();

    void createTopicAndGrantAccessFor(String topicName, RhoasTopicAccessType accessType);

    void deleteTopicAndRevokeAccessFor(String topicName, RhoasTopicAccessType accessType);
}
