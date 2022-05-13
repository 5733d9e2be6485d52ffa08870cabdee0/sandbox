package com.redhat.service.smartevents.infra.models;

import java.util.HashMap;
import java.util.Map;

public class VaultSecret {

    private String id;
    private Map<String, String> values;

    public VaultSecret() {
    }

    public VaultSecret(String id, Map<String, String> values) {
        this.id = id;
        this.values = values;
    }

    public VaultSecret(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public VaultSecret setId(String id) {
        this.id = id;
        return this;
    }

    public Map<String, String> getValues() {
        return values;
    }

    public VaultSecret setValues(Map<String, String> values) {
        this.values = values;
        return this;
    }

    public VaultSecret value(String key, String value) {
        if (this.values == null) {
            this.values = new HashMap<>();
        }
        this.values.put(key, value);
        return this;
    }
}