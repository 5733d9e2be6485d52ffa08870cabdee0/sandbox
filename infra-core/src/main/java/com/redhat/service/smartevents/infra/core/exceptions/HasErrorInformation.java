package com.redhat.service.smartevents.infra.core.exceptions;

public interface HasErrorInformation {

    Integer getErrorId();

    String getErrorUUID();
}
