package com.redhat.service.smartevents.executor;

import java.util.Map;

import com.redhat.service.smartevents.infra.v1.api.models.dto.ProcessorDTO;

import io.cloudevents.CloudEvent;

public interface Executor {

    ProcessorDTO getProcessor();

    void onEvent(CloudEvent event, Map<String, String> headers);
}
