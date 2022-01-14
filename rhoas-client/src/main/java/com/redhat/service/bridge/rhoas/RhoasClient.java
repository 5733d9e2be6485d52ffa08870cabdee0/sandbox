package com.redhat.service.bridge.rhoas;

import com.redhat.service.bridge.rhoas.dto.TopicAndServiceAccountRequest;
import com.redhat.service.bridge.rhoas.dto.TopicAndServiceAccountResponse;

import io.smallrye.mutiny.Uni;

public interface RhoasClient {

    Uni<TopicAndServiceAccountResponse> createTopicAndConsumerServiceAccount(TopicAndServiceAccountRequest request);
}
