package com.redhat.service.smartevents.infra.core.exceptions;

import java.util.Optional;

public interface CompositeBridgeErrorService {

    Optional<BridgeError> getError(Class clazz);

}
