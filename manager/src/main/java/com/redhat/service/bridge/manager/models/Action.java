package com.redhat.service.bridge.manager.models;

import java.util.Map;
import java.util.UUID;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;

import com.redhat.service.bridge.infra.models.actions.ActionFactory;
import com.redhat.service.bridge.infra.models.actions.BaseAction;

@Entity
public class Action {

    @Id
    private String id = UUID.randomUUID().toString();

    private String name;

    private String type;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "ACTION_PARAMETER",
            joinColumns = @JoinColumn(name = "action_id"))
    @MapKeyColumn(name = "name")
    @Column(name = "value", nullable = false, updatable = false)
    private Map<String, String> parameters;

    public String getId() {
        return id;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BaseAction toActionRequest() {
        return ActionFactory.buildAction(this.type, this.name, this.parameters);
    }
}
