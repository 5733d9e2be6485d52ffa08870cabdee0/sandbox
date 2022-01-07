package com.redhat.service.bridge.rhoas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.openshift.cloud.api.kas.auth.models.Topic;
import com.openshift.cloud.api.kas.models.ServiceAccount;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicAndServiceAccountResponse {

    private Topic topic;
    private ServiceAccount serviceAccount;

    public TopicAndServiceAccountResponse() {
    }

    public TopicAndServiceAccountResponse(Topic topic, ServiceAccount serviceAccount) {
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
}
