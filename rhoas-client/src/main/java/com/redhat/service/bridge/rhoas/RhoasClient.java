package com.redhat.service.bridge.rhoas;

import com.redhat.service.bridge.rhoas.dto.TopicAndServiceAccountRequest;
import com.redhat.service.bridge.rhoas.dto.TopicAndServiceAccountResponse;

import io.smallrye.mutiny.Uni;

public interface RhoasClient {

    String ENABLED_FLAG = "event-bridge.feature-flags.rhoas-enabled";

    Uni<TopicAndServiceAccountResponse> createTopicAndConsumerServiceAccount(TopicAndServiceAccountRequest request);

}
