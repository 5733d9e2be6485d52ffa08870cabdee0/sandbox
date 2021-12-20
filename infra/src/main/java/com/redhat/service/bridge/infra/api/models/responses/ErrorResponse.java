package com.redhat.service.bridge.infra.api.models.responses;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.exceptions.Error;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse extends BaseResponse {

    @JsonProperty("code")
    private String code;

    @JsonProperty("reason")
    private String reason;

    public static ErrorResponse from(Error error) {
        ErrorResponse response = new ErrorResponse();
        response.setId(Integer.toString(error.getId()));
        response.setCode(error.getCode());
        response.setReason(error.getReason());
        response.setHref(APIConstants.ERROR_API_BASE_PATH + error.getId());
        return response;
    }

    protected ErrorResponse() {
        super("Error");
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
        return Objects.hash(code, reason);
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
        return Objects.equals(code, other.code) && Objects.equals(reason, other.reason);
    }
}
