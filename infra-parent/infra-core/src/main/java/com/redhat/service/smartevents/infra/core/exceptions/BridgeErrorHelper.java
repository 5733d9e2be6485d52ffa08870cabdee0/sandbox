package com.redhat.service.smartevents.infra.core.exceptions;

public interface BridgeErrorHelper {

    BridgeErrorInstance getBridgeErrorInstance(Exception e);

    BridgeErrorInstance getBridgeErrorInstance(int errorId, String errorUUID);

    String makeUserMessage(HasErrorInformation hasErrorInformation);

}
