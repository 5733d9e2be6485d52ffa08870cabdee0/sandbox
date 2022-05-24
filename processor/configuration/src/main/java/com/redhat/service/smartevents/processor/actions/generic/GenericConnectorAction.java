package com.redhat.service.smartevents.processor.actions.generic;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.AbstractGatewayConnector;

public abstract class GenericConnectorAction extends AbstractGatewayConnector<Action> {

    public GenericConnectorAction(String connectorTypeId) {
        super(ConnectorType.SINK, connectorTypeId);
    }

    @Override
    protected void addConnectorSpecificPayload(Action gateway, String topicName, ObjectNode definition) {

        ObjectNode rawParameters = gateway.getParameters();

        definition.setAll(rawParameters);
        definition.set(CONNECTOR_TOPIC_PARAMETER, new TextNode(topicName));

        System.out.println("Connector payload:\n" + definition);
    }
}
