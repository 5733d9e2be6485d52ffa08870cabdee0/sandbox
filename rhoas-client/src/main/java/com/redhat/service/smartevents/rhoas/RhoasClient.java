package com.redhat.service.smartevents.rhoas;

import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.Topic;
import com.openshift.cloud.api.kas.models.ServiceAccount;
import com.openshift.cloud.api.kas.models.ServiceAccountRequest;

import io.smallrye.mutiny.Uni;

public interface RhoasClient {

    Uni<ServiceAccount> createServiceAccount(ServiceAccountRequest serviceAccountRequest);

    Uni<Void> deleteServiceAccount(String id);

    Uni<Topic> getTopic(String topicName);

    Uni<Topic> createTopic(NewTopicInput newTopicInput);

    Uni<Topic> createTopicAndGrantAccess(NewTopicInput newTopicInput, String userId, RhoasTopicAccessType accessType);

    Uni<Void> deleteTopic(String topicName);

    Uni<Void> deleteTopicAndRevokeAccess(String topicName, String userId, RhoasTopicAccessType accessType);

    Uni<Void> grantAccess(String topicName, String userId, RhoasTopicAccessType accessType);

    Uni<Void> revokeAccess(String topicName, String userId, RhoasTopicAccessType accessType);
}
