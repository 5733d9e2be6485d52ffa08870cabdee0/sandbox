package com.redhat.service.smartevents.manager;

import javax.inject.Inject;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.manager.config.ConfigurationLoader;
import com.redhat.service.smartevents.manager.models.OrganisationServiceLimit;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class LimitServiceTest {

    @Inject
    LimitService limitService;

    @InjectMock
    ObjectMapper mapper;

    @InjectMock
    ConfigurationLoader configurationLoader;

    @Test
    public void testGetOrganisationServiceLimit() {
        String orgId = "15247674";
        OrganisationServiceLimit organisationServiceLimit = limitService.getOrganisationServiceLimit(orgId);
        assertThat(organisationServiceLimit).isNotNull();
    }
}
