package com.redhat.service.bridge.infra.exceptions;

public class Error {

    private final int id;
    private final String code;
    private final String reason;
    private final boolean isUserException;

    public Error(int id, String code, String reason, boolean isUserException) {
        super();
        this.id = id;
        this.code = code;
        this.reason = reason;
        this.isUserException = isUserException;
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

    public boolean isUserException() {
        return isUserException;
    }

    @Override
    public String toString() {
        return "Error [id=" + id + ", code=" + code + ", reason=" + reason + "]";
    }
}
