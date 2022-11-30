package com.redhat.service.smartevents.integration.tests.resources.webhook.site;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class WebhookSiteRequest {

    private String uuid;

    private Map<String, List<String>> headers;

    private String content;

    private LocalDateTime createdAt;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
