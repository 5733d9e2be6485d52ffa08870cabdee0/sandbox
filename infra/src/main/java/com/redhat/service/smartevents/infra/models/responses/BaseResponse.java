package com.redhat.service.smartevents.infra.models.responses;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ObjectReference")
public abstract class BaseResponse {

    @NotNull
    @JsonProperty("kind")
    private final String kind;

    @NotNull
    @JsonProperty("id")
    protected String id;

    @JsonProperty("name")
    protected String name;

    @NotNull
    @JsonProperty("href")
    protected String href;

    protected BaseResponse(String kind) {
        this.kind = kind;
    }

    public String getKind() {
        return kind;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHref() {
        return href;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
