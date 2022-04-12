package com.redhat.service.rhose.manager;

import com.openshift.cloud.api.kas.auth.models.Topic;
import com.redhat.service.rhose.rhoas.RhoasTopicAccessType;

public interface RhoasService {

    Topic createTopicAndGrantAccessFor(String topicName, RhoasTopicAccessType accessType);

    void deleteTopicAndRevokeAccessFor(String topicName, RhoasTopicAccessType accessType);
}
