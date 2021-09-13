package com.redhat.service.bridge.executor;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.dto.ProcessorDTO;

import io.quarkus.test.junit.QuarkusTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
public class ExecutorFactoryTest {

    @Inject
    ExecutorFactory executorFactory;

    @Test
    public void createExecutor() {
        Executor e = executorFactory.createExecutor(new ProcessorDTO());
        assertThat(e, is(notNullValue()));
    }
}
