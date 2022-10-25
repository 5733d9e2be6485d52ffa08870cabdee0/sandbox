package com.redhat.service.smartevents.manager.ams;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.TestConstants;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class ProcessorQuotaServiceTest {

    @Inject
    ProcessorsQuotaService processorsQuotaService;

    @Test
    public void testProcessorQuotaService() {
        assertThat(processorsQuotaService.getProcessorsQuota(TestConstants.DEFAULT_ORGANISATION_ID)).isEqualTo(100);
        assertThat(processorsQuotaService.getProcessorsQuota("")).isEqualTo(0);
    }
}
