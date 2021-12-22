package com.redhat.service.bridge.rhoas.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicAndServiceAccountResponse {

    private String topicName;
    private String serviceAccountName;
    private String serviceAccountId;
    private String serviceAccountClientId;
    private String serviceAccountClientSecret;

    public TopicAndServiceAccountResponse() {
    }

    public TopicAndServiceAccountResponse(String topicName, String serviceAccountName, String serviceAccountId, String serviceAccountClientId, String serviceAccountClientSecret) {
        this.topicName = topicName;
        this.serviceAccountName = serviceAccountName;
        this.serviceAccountId = serviceAccountId;
        this.serviceAccountClientId = serviceAccountClientId;
        this.serviceAccountClientSecret = serviceAccountClientSecret;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getServiceAccountName() {
        return serviceAccountName;
    }

    public void setServiceAccountName(String serviceAccountName) {
        this.serviceAccountName = serviceAccountName;
    }

    public String getServiceAccountId() {
        return serviceAccountId;
    }

    public void setServiceAccountId(String serviceAccountId) {
        this.serviceAccountId = serviceAccountId;
    }

    public String getServiceAccountClientId() {
        return serviceAccountClientId;
    }

    public void setServiceAccountClientId(String serviceAccountClientId) {
        this.serviceAccountClientId = serviceAccountClientId;
    }

    public String getServiceAccountClientSecret() {
        return serviceAccountClientSecret;
    }

    public void setServiceAccountClientSecret(String serviceAccountClientSecret) {
        this.serviceAccountClientSecret = serviceAccountClientSecret;
    }

    @Override
    public String toString() {
        return "TopicAndServiceAccount{" +
                "topicName='" + topicName + '\'' +
                ", serviceAccountName='" + serviceAccountName + '\'' +
                ", serviceAccountId='" + serviceAccountId + '\'' +
                ", serviceAccountClientId='" + serviceAccountClientId + '\'' +
                ", serviceAccountClientSecret='" + serviceAccountClientSecret + '\'' +
                '}';
    }
}
