package com.redhat.service.smartevents.executor;

import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;

public interface Executor {

    void onEvent(String event);

    ProcessorDTO getProcessor();

}
