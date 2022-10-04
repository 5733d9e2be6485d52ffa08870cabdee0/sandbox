package com.redhat.service.smartevents.infra.exceptions;

public interface HasErrorInformation {

    Integer getErrorId();

    String getErrorUUID();
}
