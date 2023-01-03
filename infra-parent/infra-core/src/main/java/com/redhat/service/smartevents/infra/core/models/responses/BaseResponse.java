package com.redhat.service.smartevents.infra.core.models.responses;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ObjectReference")
public abstract class BaseResponse {

    @NotNull
    @JsonProperty("kind")
    @Schema(
            description = "The kind (type) of this resource")
    private final String kind;

    @NotNull
    @JsonProperty("id")
    @Schema(
            description = "The unique identifier of this resource")
    protected String id;

    @JsonProperty("name")
    @Schema(
            description = "The name of this resource",
            example = "resourceName1")
    protected String name;

    @NotNull
    @JsonProperty("href")
    @Schema(
            description = "The URL of this resource, without the protocol",
            example = "example.com/resource")
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
