package com.redhat.service.smartevents.processor.actions.eventhubs;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface AzureEventHubsAction extends GatewayBean {

    String TYPE = "azure_eventhubs_sink_0.1";

    @Override
    default String getType() {
        return TYPE;
    }
}
