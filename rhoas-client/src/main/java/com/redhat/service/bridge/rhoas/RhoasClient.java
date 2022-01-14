package com.redhat.service.bridge.rhoas;

import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.Topic;
import com.redhat.service.bridge.rhoas.dto.TopicAndServiceAccountRequest;
import com.redhat.service.bridge.rhoas.dto.TopicAndServiceAccountResponse;

import io.smallrye.mutiny.Uni;

public interface RhoasClient {

    Uni<Topic> createTopic(NewTopicInput request);

    Uni<TopicAndServiceAccountResponse> createTopicAndConsumerServiceAccount(TopicAndServiceAccountRequest request);
}
