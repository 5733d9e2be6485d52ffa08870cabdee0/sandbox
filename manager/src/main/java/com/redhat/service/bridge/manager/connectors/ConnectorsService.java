package com.redhat.service.bridge.manager.connectors;

import java.util.Optional;

import com.redhat.service.bridge.infra.models.actions.BaseAction;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.models.Processor;

public interface ConnectorsService {

    Optional<String> createConnectorIfNeeded(ProcessorRequest processorRequest, BaseAction resolvedAction, Processor processor);
}
