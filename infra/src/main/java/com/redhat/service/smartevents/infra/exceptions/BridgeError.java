package com.redhat.service.smartevents.infra.exceptions;

import java.util.Objects;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema
public class BridgeError {

    private final int id;
    private final String code;
    private final String reason;
    private final BridgeErrorType type;

    public BridgeError(int id, String code, String reason, BridgeErrorType type) {
        super();
        this.id = id;
        this.code = code;
        this.reason = reason;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getReason() {
        return reason;
    }

    public BridgeErrorType getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BridgeError)) {
            return false;
        }
        BridgeError that = (BridgeError) o;
        return getId() == that.getId() && Objects.equals(getCode(), that.getCode()) && Objects.equals(getReason(), that.getReason()) && getType() == that.getType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCode(), getReason(), getType());
    }

    @Override
    public String toString() {
        return "Error [id=" + id + ", code=" + code + ", reason=" + reason + ", type=" + type + "]";
    }
}
