package com.redhat.service.smartevents.integration.tests.resources.webhook.site;

public enum WebhookSiteQuerySorting {
    OLDEST("oldest"),
    NEWEST("newest");

    private String value;

    WebhookSiteQuerySorting(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
