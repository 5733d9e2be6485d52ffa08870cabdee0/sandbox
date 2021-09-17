package com.redhat.service.bridge.infra.k8s;

public class ResourceEvent {

    private String subject;

    private String resourceId;

    private Action action;

    public ResourceEvent(String subject, String resourceId, Action action) {
        this.subject = subject;
        this.resourceId = resourceId;
        this.action = action;
    }

    public String getSubject() {
        return subject;
    }

    public String getResourceId() {
        return resourceId;
    }

    public Action getAction() {
        return action;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setResource(String resourceId) {
        this.resourceId = resourceId;
    }

    public void setAction(Action action) {
        this.action = action;
    }
}
