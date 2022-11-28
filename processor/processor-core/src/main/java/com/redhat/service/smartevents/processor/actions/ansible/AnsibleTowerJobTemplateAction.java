package com.redhat.service.smartevents.processor.actions.ansible;

import com.redhat.service.smartevents.processor.GatewayBean;
import com.redhat.service.smartevents.processor.actions.webhook.WebhookAction;

public interface AnsibleTowerJobTemplateAction extends GatewayBean {

    String TYPE = "ansible_tower_job_template_sink_0.1";
    String ENDPOINT_PARAM = WebhookAction.ENDPOINT_PARAM;
    String JOB_TEMPLATE_ID_PARAM = "job_template_id";
    String BASIC_AUTH_USERNAME_PARAM = WebhookAction.BASIC_AUTH_USERNAME_PARAM;
    String BASIC_AUTH_PASSWORD_PARAM = WebhookAction.BASIC_AUTH_PASSWORD_PARAM;
    String SSL_VERIFICATION_DISABLED = WebhookAction.SSL_VERIFICATION_DISABLED;

    @Override
    default String getType() {
        return TYPE;
    }

}

