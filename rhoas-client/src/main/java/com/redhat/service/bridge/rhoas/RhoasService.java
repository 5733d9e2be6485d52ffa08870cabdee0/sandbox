package com.redhat.service.bridge.rhoas;

import com.redhat.service.bridge.rhoas.dto.TopicAndServiceAccount;

import io.smallrye.mutiny.Uni;

public interface RhoasService {

    Uni<TopicAndServiceAccount> createTopicAndConsumerServiceAccount(String topicName, String serviceAccountName);

}
