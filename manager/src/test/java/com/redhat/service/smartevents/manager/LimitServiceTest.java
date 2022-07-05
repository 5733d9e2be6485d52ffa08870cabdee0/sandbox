package com.redhat.service.smartevents.manager;

import java.util.Optional;

import javax.inject.Inject;

import org.junit.Test;

import com.redhat.service.smartevents.manager.models.InstanceLimit;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class LimitServiceTest {

    @Inject
    LimitService limitService;

    @Test
    public void testGetOrganisationServiceLimit() {
        String orgId = "15247674";
        Optional<InstanceLimit> organisationInstanceLimit = limitService.getOrganisationInstanceLimit(orgId);
        assertThat(organisationInstanceLimit).isPresent();
    }
}
