package com.redhat.service.bridge.manager.models;

import com.redhat.service.bridge.rhoas.dto.ServiceAccount;
import com.redhat.service.bridge.rhoas.dto.Topic;

public class TopicAndServiceAccount {

    private Topic topic;
    private ServiceAccount serviceAccount;

    public TopicAndServiceAccount() {
    }

    public TopicAndServiceAccount(Topic topic, ServiceAccount serviceAccount) {
        this.topic = topic;
        this.serviceAccount = serviceAccount;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public ServiceAccount getServiceAccount() {
        return serviceAccount;
    }

    public void setServiceAccount(ServiceAccount serviceAccount) {
        this.serviceAccount = serviceAccount;
    }

    @Override
    public String toString() {
        return "TopicAndServiceAccount{" +
                "topic=" + topic +
                ", serviceAccount=" + serviceAccount +
                '}';
    }
}
