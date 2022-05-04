package com.redhat.service.smartevents.executor;

import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;

import io.cloudevents.CloudEvent;

public interface Executor {

    ProcessorDTO getProcessor();

    void onEvent(CloudEvent event);
}
