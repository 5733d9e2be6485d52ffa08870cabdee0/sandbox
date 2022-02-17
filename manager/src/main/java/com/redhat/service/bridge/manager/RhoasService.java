package com.redhat.service.bridge.manager;

import com.openshift.cloud.api.kas.auth.models.Topic;
import com.redhat.service.bridge.rhoas.RhoasTopicAccessType;

public interface RhoasService {

    Topic createTopicAndGrantAccessFor(String topicName, RhoasTopicAccessType accessType);

    void deleteTopicAndRevokeAccessFor(String topicName, RhoasTopicAccessType accessType);
}
