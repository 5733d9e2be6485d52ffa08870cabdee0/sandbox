package com.redhat.service.bridge.manager.connectors;

import java.util.List;

import com.redhat.service.bridge.manager.models.ConnectorEntity;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.Message;

public interface ConnectorsOrchestrator {

    Uni<List<Message<ConnectorEntity>>> updatePendingConnectors();

}
