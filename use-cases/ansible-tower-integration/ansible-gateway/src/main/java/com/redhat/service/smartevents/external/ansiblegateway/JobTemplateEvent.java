package com.redhat.service.smartevents.external.ansiblegateway;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JobTemplateEvent {
    @JsonProperty("job_template_id")
    public Integer jobTemplateId;
}
