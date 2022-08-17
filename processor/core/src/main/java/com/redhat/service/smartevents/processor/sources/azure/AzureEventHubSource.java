package com.redhat.service.smartevents.processor.sources.azure;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface AzureEventHubSource extends GatewayBean {

    String TYPE = "azure_eventhubs_source_0.1";

    String AZURE_NAMESPACE_NAME = "azure_namespace_name";
    String AZURE_EVENTHUB_NAME = "azure_eventhub_name";
    String AZURE_SHARED_ACCESS_NAME = "azure_shared_access_name";
    String AZURE_SHARD_ACCESS_KEY = "azure_shared_access_key";
    String AZURE_BLOB_ACCOUNT_NAME = "azure_blob_account_name";
    String AZURE_BLOB_ACCESS_KEY = "azure_blob_access_key";
    String AZURE_BLOB_CONTAINER_NAME = "azure_blob_container_name";

    @Override
    default String getType() {
        return TYPE;
    }
}
