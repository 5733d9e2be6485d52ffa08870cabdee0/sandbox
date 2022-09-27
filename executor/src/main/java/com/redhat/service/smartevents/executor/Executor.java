package com.redhat.service.smartevents.executor;

import java.util.Map;

import com.redhat.service.smartevents.infra.api.v1.models.dto.ProcessorDTO;

import io.cloudevents.CloudEvent;

public interface Executor {

    ProcessorDTO getProcessor();

    void onEvent(CloudEvent event, Map<String, String> headers);
}
