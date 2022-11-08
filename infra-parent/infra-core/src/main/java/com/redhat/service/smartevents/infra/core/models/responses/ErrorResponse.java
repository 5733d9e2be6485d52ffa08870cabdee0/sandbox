package com.redhat.service.smartevents.infra.core.models.responses;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.smartevents.infra.core.exceptions.BridgeError;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Error")
public class ErrorResponse extends BaseResponse {

    @JsonProperty("code")
    private String code;

    @NotNull
    @JsonProperty("reason")
    private String reason;

    public static ErrorResponse from(BridgeError bridgeError) {
        ErrorResponse response = new ErrorResponse();
        response.setId(Integer.toString(bridgeError.getId()));
        response.setCode(bridgeError.getCode());
        response.setReason(bridgeError.getReason());
        return response;
    }

    protected ErrorResponse() {
        super("Error");
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getHref() {
        return href;
    }

    @Override
    public void setHref(String href) {
        this.href = href;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, href, name, code, reason);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ErrorResponse other = (ErrorResponse) obj;
        return Objects.equals(id, other.id) && Objects.equals(href, other.href) && Objects.equals(name, other.name) && Objects.equals(code, other.code) && Objects.equals(reason, other.reason);
    }
}
