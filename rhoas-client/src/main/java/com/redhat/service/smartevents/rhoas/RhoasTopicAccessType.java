package com.redhat.service.smartevents.rhoas;

public enum RhoasTopicAccessType {
    CONSUMER("consumer"),
    PRODUCER("producer"),
    CONSUMER_AND_PRODUCER("consumer and producer");

    private final String text;

    RhoasTopicAccessType(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
