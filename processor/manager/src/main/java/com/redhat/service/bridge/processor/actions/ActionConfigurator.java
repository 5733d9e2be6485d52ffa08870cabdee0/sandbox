package com.redhat.service.bridge.processor.actions;

import java.util.Optional;

public interface ActionConfigurator {

    ActionValidator getValidator(String actionType);

    Optional<ActionResolver> getResolver(String actionType);

    Optional<ActionConnector> getConnector(String actionType);
}
