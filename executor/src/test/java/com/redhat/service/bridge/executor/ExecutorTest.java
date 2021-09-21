package com.redhat.service.bridge.executor;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.models.dto.ProcessorDTO;
import com.redhat.service.bridge.infra.models.filters.StringEquals;

public class ExecutorTest {

    @Test
    public void testExecutor() {
        ProcessorDTO processorDTO = new ProcessorDTO();
        processorDTO.setFilters(Collections.singleton(new StringEquals()));
        Executor executor = new Executor(processorDTO, new TemplateFactoryFEEL(), new FilterEvaluatorFEEL());
    }

}
