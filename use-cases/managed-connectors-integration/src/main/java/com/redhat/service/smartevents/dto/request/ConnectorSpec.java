package com.redhat.service.smartevents.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConnectorSpec {


    /**
     * Slack Sink
     * <p>
     *
     *
     */
    @JsonProperty("connector")
    private SlackConnector connector;

    @JsonProperty("kafka")
    private ConnectorSpecKafka connectorSpecKafka;

    @JsonProperty("kafka")
    public ConnectorSpecKafka getConnectorSpecKafka() {
        return connectorSpecKafka;
    }

    @JsonProperty("kafka")
    public void setConnectorSpecKafka(ConnectorSpecKafka connectorSpecKafka) {
        this.connectorSpecKafka = connectorSpecKafka;
    }

    /**
     * Slack Sink
     * <p>
     *
     *
     */
    @JsonProperty("connector")
    public SlackConnector getConnector() {
        return connector;
    }

    /**
     * Slack Sink
     * <p>
     *
     *
     */
    @JsonProperty("connector")
    public void setConnector(SlackConnector connector) {
        this.connector = connector;
    }

    @Override
    public String toString() {
        return "ConnectorSpec{" +
                "connector=" + connector +
                ", connectorSpecKafka=" + connectorSpecKafka +
                '}';
    }
}
