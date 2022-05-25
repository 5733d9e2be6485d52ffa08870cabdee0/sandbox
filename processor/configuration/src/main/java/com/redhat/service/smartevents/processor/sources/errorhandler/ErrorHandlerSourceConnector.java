package com.redhat.service.smartevents.processor.sources.errorhandler;

import javax.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.redhat.service.smartevents.infra.models.connectors.ConnectorType;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.AbstractGatewayConnector;

@ApplicationScoped
public class ErrorHandlerSourceConnector extends AbstractGatewayConnector<Source> implements ErrorHandlerSource {

    public static final ConnectorType CONNECTOR_TYPE = ConnectorType.SOURCE;
    public static final String CONNECTOR_TYPE_ID = "error_handler_0.1";

    public ErrorHandlerSourceConnector() {
        super(CONNECTOR_TYPE, CONNECTOR_TYPE_ID);
    }

    @Override
    protected void addConnectorSpecificPayload(Source source, String topicName, ObjectNode definition) {
        definition.set(CONNECTOR_TOPIC_PARAMETER, new TextNode(topicName));
    }

    @Override
    public boolean hasInternalRouting() {
        return true;
    }
}
