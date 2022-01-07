package com.redhat.service.bridge.rhoas;

import com.openshift.cloud.api.kas.auth.models.AclBinding;
import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.Topic;

import io.smallrye.mutiny.Uni;

public interface KafkaInstanceAdminClient {

    Uni<Void> createAcl(AclBinding aclBinding);

    Uni<Void> deleteAcl(AclBinding aclBinding);

    Uni<Topic> createTopic(NewTopicInput newTopicInput);

    Uni<Void> deleteTopic(String topicName);

}
