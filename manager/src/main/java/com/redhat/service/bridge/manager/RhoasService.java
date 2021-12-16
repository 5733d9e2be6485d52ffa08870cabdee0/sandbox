package com.redhat.service.bridge.manager;

import com.redhat.service.bridge.manager.models.TopicAndServiceAccount;

import io.smallrye.mutiny.Uni;

public interface RhoasService {

    Uni<TopicAndServiceAccount> createTopicAndServiceAccount(String topicName, String serviceAccountName);

}
