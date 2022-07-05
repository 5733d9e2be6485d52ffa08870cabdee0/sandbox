package com.redhat.service.smartevents.manager;

import java.util.Optional;

import javax.inject.Inject;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ServiceLimitExceedException;
import com.redhat.service.smartevents.manager.models.Bridge;
import com.redhat.service.smartevents.manager.models.LimitInstanceType;
import com.redhat.service.smartevents.manager.utils.Fixtures;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.models.InstanceLimit;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(bridgesService.getActiveBridgeCount(orgId))
                .thenReturn(1L);
        Optional<InstanceLimit> organisationInstanceLimit = limitService.getOrganisationInstanceLimit(orgId);
        assertThat(organisationInstanceLimit).isPresent();
        assertThat(organisationInstanceLimit.get().getInstanceType()).isEqualTo(LimitInstanceType.EVAL);
    }

    @Test
    public void testGetOrganisationServiceLimit_LimitExceed() {

        String orgId = "15247674";
        when(bridgesService.getActiveBridgeCount(orgId))
                .thenReturn(6L);
        Assertions.assertThrows(ServiceLimitExceedException.class, () -> {limitService.getOrganisationInstanceLimit(orgId);}, "Max allowed bridge instance limit exceed");
    }

    @Test
    public void testGetBridgeInstanceLimit() {
        String bridgeId = "123";
        Bridge bridge = Fixtures.createBridge();
        when(bridgesService.getBridge(bridgeId)).thenReturn(bridge);
        Optional<InstanceLimit> optInstanceLimit = limitService.getBridgeInstanceLimit(bridgeId);
        assertThat(optInstanceLimit).isPresent();
        InstanceLimit instanceLimit = optInstanceLimit.get();
        assertThat(instanceLimit.getInstanceType()).isEqualByComparingTo(LimitInstanceType.EVAL);
    }
}
