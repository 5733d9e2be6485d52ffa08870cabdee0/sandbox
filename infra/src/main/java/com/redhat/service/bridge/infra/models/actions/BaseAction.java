package com.redhat.service.bridge.infra.models.actions;

import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = BaseAction.ACTION_TYPE_FIELD)
@JsonSubTypes({
        @JsonSubTypes.Type(value = KafkaTopicAction.class, name = KafkaTopicAction.KAFKA_ACTION_TYPE),
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class BaseAction {
    public static final String ACTION_TYPE_FIELD = "type";

    @NotNull(message = "An Action must have a name")
    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = ACTION_TYPE_FIELD)
    protected String type;

    public BaseAction() {
    }

    public BaseAction(String name) {
        this.name = name;
    }

    public BaseAction(String name, Map<String, String> parameters) {
        this.name = name;
        setParameters(parameters);
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

    @JsonIgnore
    public abstract Map<String, String> getParameters();

    public abstract void setParameters(Map<String, String> parameters);
}
