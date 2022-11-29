package com.redhat.service.smartevents.manager.v1.ams;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.manager.core.ams.OrganisationQuota;

import io.quarkus.runtime.Quarkus;

@ApplicationScoped
public class QuotaConfigurationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaConfigurationProvider.class);

    @ConfigProperty(name = "event-bridge.account-management-service.quota.config")
    String accountManagementServiceQuotaConfig;

    Map<String, OrganisationQuota> organisationsQuota;

    @Inject
    ObjectMapper objectMapper;

    @PostConstruct
    void init() {
        try {
            this.organisationsQuota = objectMapper.readValue(accountManagementServiceQuotaConfig, new TypeReference<HashMap<String, OrganisationQuota>>() {
            });
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not read organisations quota configuration. The application is going to be stopped.");
            Quarkus.asyncExit(1);
        }
    }

    public OrganisationQuota getOrganisationQuotas(String organisationId) {
        return organisationsQuota.getOrDefault(organisationId, new OrganisationQuota(0, 0));
    }
}
