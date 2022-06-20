package com.redhat.service.smartevents.infra.models;

import java.util.HashMap;
import java.util.Map;

public class EventBridgeSecret {

    private String id;
    private Map<String, String> values;

    public EventBridgeSecret() {
    }

    public EventBridgeSecret(String id, Map<String, String> values) {
        this.id = id;
        this.values = values;
    }

    public EventBridgeSecret(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public EventBridgeSecret setId(String id) {
        this.id = id;
        return this;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public EventBridgeSecret setValues(Map<String, String> values) {
        this.values = values;
        return this;
    }

    public EventBridgeSecret value(String key, String value) {
        if (this.values == null) {
            this.values = new HashMap<>();
        }
        this.values.put(key, value);
        return this;
    }
}