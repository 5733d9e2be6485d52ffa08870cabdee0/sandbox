package com.redhat.service.smartevents.processor.actions.eventhubs;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface AzureEventHubsAction extends GatewayBean {

    String TYPE = "azure_eventhubs_sink_0.1";
    String AZURE_NAMESPACE_NAME_PARAM = "azure_namespace_name";
    String AZURE_EVENTHUB_NAME_PARAM = "azure_eventhub_name";
    String AZURE_SHARED_ACCESS_NAME_PARAM = "azure_shared_access_name";
    String AZURE_SHARED_ACCESS_KEY_PARAM = "azure_shared_access_key";

    @Override
    default String getType() {
        return TYPE;
    }
}
