package com.redhat.service.smartevents.manager.core.services;

import com.openshift.cloud.api.kas.auth.models.Topic;
import com.redhat.service.smartevents.rhoas.RhoasTopicAccessType;

public interface RhoasService {

    Topic createTopicAndGrantAccessFor(String topicName, RhoasTopicAccessType accessType);

    void deleteTopicAndRevokeAccessFor(String topicName, RhoasTopicAccessType accessType);
}
