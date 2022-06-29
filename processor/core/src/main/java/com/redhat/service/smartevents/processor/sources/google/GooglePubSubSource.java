package com.redhat.service.smartevents.processor.sources.google;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface GooglePubSubSource extends GatewayBean {

    String TYPE = "google_pubsub_source_0.1";

    String GCP_PROJECT_ID_PARAM = "gcp_project_id";
    String GCP_SUBSCRIPTION_NAME = "gcp_subscription_name";
    String GCP_SERVICE_ACCOUNT_KEY_PARAM = "gcp_service_account_key";

    @Override
    default String getType() {
        return TYPE;
    }
}
