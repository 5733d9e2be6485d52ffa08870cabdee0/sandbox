package com.redhat.service.smartevents.manager;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ServiceLimitException;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.QuotaLimit;
import com.redhat.service.smartevents.manager.models.QuotaType;
import com.redhat.service.smartevents.manager.utils.Fixtures;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@QuarkusTest
public class LimitServiceTest {

    @Inject
    LimitService limitService;

    @InjectMock
    BridgesService bridgesService;

    @Test
    public void testGetOrganisationServiceLimit_withInLimit() {

        String orgId = "15247674";
        when(bridgesService.getBridgeCount(orgId, QuotaType.EVAL))
                .thenReturn(1L);
        QuotaLimit organisationInstanceLimit = limitService.getOrganisationQuotaLimit(orgId);
        assertThat(organisationInstanceLimit.getQuotaType()).isEqualTo(QuotaType.EVAL);
    }

    @Test
    public void testGetOrganisationServiceLimit_LimitExceed() {

        String orgId = "15247674";
        when(bridgesService.getBridgeCount(orgId, QuotaType.EVAL))
                .thenReturn(36L);
        assertThatThrownBy(() -> {
            limitService.getOrganisationQuotaLimit(orgId);
        }).isInstanceOf(ServiceLimitException.class).hasMessage("Max allowed bridge instance limit exceed");
    }

    @Test
    public void testGetBridgeInstanceLimit() {
        Bridge bridge = Fixtures.createBridge();
        QuotaLimit quotaLimit = limitService.getBridgeQuotaLimit(bridge);
        assertThat(quotaLimit.getQuotaType()).isEqualByComparingTo(QuotaType.EVAL);
    }
}
