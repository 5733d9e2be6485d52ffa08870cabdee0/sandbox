package com.redhat.service.smartevents.infra.exceptions;

public interface HasBridgeErrorInformation {

    Integer getBridgeErrorId();

    String getBridgeErrorUUID();
}
