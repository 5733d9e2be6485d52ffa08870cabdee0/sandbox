package com.redhat.service.smartevents.processor.actions.google;

import com.redhat.service.smartevents.processor.GatewayBean;

public interface GooglePubSubAction extends GatewayBean {

    String TYPE = "google_pubsub_sink_0.1";

    String GCP_PROJECT_ID_PARAM = "gcp_project_id";
    String GCP_DESTINATION_NAME_PARAM = "gcp_destination_name";
    String GCP_SERVICE_ACCOUNT_KEY_PARAM = "gcp_service_account_key";

    @Override
    default String getType() {
        return TYPE;
    }
}
