package com.redhat.service.smartevents.processor.actions.generic;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.AbstractGatewayConnector;

@ApplicationScoped
public class GenericActionConnector extends AbstractGatewayConnector<Action> {

    public GenericActionConnector() {
        super(ConnectorType.SINK, "connectorTypeId");
    }

    @Override
    protected void addConnectorSpecificPayload(Action gateway, String topicName, ObjectNode definition) {

        ObjectNode rawParameters = gateway.getRawParameters();
        definition.setAll(rawParameters);

        definition.set(CONNECTOR_TOPIC_PARAMETER, new TextNode(topicName));
    }

    @Override
    public String getType() {
        return "Generic";
    }
}
