package com.redhat.service.smartevents.infra.v1.api.models.filters;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = BaseFilter.FILTER_TYPE_FIELD)
@JsonSubTypes({
        @JsonSubTypes.Type(value = StringBeginsWith.class, name = StringBeginsWith.FILTER_TYPE_NAME),
        @JsonSubTypes.Type(value = StringContains.class, name = StringContains.FILTER_TYPE_NAME),
        @JsonSubTypes.Type(value = StringEquals.class, name = StringEquals.FILTER_TYPE_NAME),
        @JsonSubTypes.Type(value = NumberIn.class, name = NumberIn.FILTER_TYPE_NAME),
        @JsonSubTypes.Type(value = StringIn.class, name = StringIn.FILTER_TYPE_NAME)
})
@JsonIgnoreProperties(ignoreUnknown = true)
// See https://issues.redhat.com/browse/MGDOBR-638
// Implementations *MUST* override equals(..) and hashCode() appropriately
public abstract class BaseFilter {

    public static final String FILTER_TYPE_FIELD = "type";

    @JsonProperty(FILTER_TYPE_FIELD)
    protected String type;

    @JsonProperty("key")
    @NotEmpty
    protected String key;

    protected BaseFilter(String type) {
        this.type = type;
    }

    protected BaseFilter(String type, String key) {
        this(type);
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public String getType() {
        return type;
    }

    @NotNull
    @JsonIgnore
    public abstract Object getValue();
}
