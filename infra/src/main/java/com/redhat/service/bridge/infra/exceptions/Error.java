package com.redhat.service.bridge.infra.exceptions;

public class Error {

    private final int id;
    private final String code;
    private final String reason;

    public Error(int id, String code, String reason) {
        super();
        this.id = id;
        this.code = code;
        this.reason = reason;
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

    @Override
    public String toString() {
        return "Error [id=" + id + ", code=" + code + ", reason=" + reason + "]";
    }
}
