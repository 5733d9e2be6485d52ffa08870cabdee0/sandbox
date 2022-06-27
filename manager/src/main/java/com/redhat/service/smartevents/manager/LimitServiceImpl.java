package com.redhat.service.smartevents.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.manager.config.ConfigurationLoader;
import com.redhat.service.smartevents.manager.models.*;

import io.quarkus.runtime.Startup;

@Startup
@ApplicationScoped
public class LimitServiceImpl implements LimitService {

    private static final String SERVICE_LIMIT_FILENAME = "service_limits.json";

    private ServiceLimit serviceLimit;

    @Inject
    ObjectMapper mapper;

    @Inject
    ConfigurationLoader configurationLoader;

    @PostConstruct
    void init() throws IOException {
        serviceLimit = mapper.readValue(readServiceLimitFile(), new TypeReference<>() {
        });
        validateServiceLimitJson(serviceLimit);
    }

    private String readServiceLimitFile() {
        InputStream is = configurationLoader.getConfigurationFileAsStream(SERVICE_LIMIT_FILENAME);
        return new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))
                        .lines()
                        .collect(Collectors.joining("\n"));
    }

    /**
     * Function validate the service_limit.json file using javax.validation annotation provided on {{@ServiceLimit}}
     * 
     * @param serviceLimit ServiceLimit instance read created using Json
     */
    void validateServiceLimitJson(@Valid ServiceLimit serviceLimit) {
    }

    @Override
    public OrganisationServiceLimit getOrganisationServiceLimit(String orgId) {
        Optional<OrganisationOverride> optOrgOverride = fetchOrganisationOverride(orgId);
        if (optOrgOverride.isEmpty()) {
            return createDefaultOrganisationServiceLimit();
        } else {
            return createOrganisationSpecificServiceLimit(optOrgOverride.get());
        }
    }

    private OrganisationServiceLimit createDefaultOrganisationServiceLimit() {
        ServiceLimitInstance defaultServiceLimitInstance = serviceLimit.getInstanceTypes().stream().filter(s -> serviceLimit.getDefaultInstanceType().equals(s.getInstanceType())).findFirst().get();
        OrganisationServiceLimit organisationServiceLimit = new OrganisationServiceLimit();
        organisationServiceLimit.setInstanceType(defaultServiceLimitInstance.getInstanceType());
        organisationServiceLimit.setProcessorLimit(defaultServiceLimitInstance.getProcessorLimit());
        organisationServiceLimit.setBridgeDuration(defaultServiceLimitInstance.getBridgeDuration());
        organisationServiceLimit.setInstanceQuota(defaultServiceLimitInstance.getInstanceQuota());
        return organisationServiceLimit;
    }

    private OrganisationServiceLimit createOrganisationSpecificServiceLimit(OrganisationOverride orgOverride) {
        ServiceLimitInstanceType orgInstanceType = orgOverride.getInstanceType();
        OrganisationServiceLimit orgServiceLimit = new OrganisationServiceLimit();
        orgServiceLimit.setInstanceType(orgInstanceType);

        if (orgOverride.getProcessorLimit() != 0) {
            orgServiceLimit.setProcessorLimit(orgOverride.getProcessorLimit());
        } else {
            orgServiceLimit.setProcessorLimit(fetchDefaultProcessorLimit(orgInstanceType));
        }

        if (orgOverride.getBridgeDuration() != 0) {
            orgServiceLimit.setBridgeDuration(orgOverride.getBridgeDuration());
        } else {
            orgServiceLimit.setBridgeDuration(fetchDefaultBridgeDuration(orgInstanceType));
        }

        if (orgOverride.getInstanceQuota() != 0) {
            orgServiceLimit.setInstanceQuota(orgOverride.getInstanceQuota());
        } else {
            orgServiceLimit.setInstanceQuota(fetchDefaultInstanceQuota(orgInstanceType));
        }

        return orgServiceLimit;
    }

    private Optional<OrganisationOverride> fetchOrganisationOverride(String orgId) {
        return serviceLimit.getOrganisationOverrides().stream().filter(s -> orgId.equals(s.getOrgId())).findAny();
    }

    private int fetchDefaultProcessorLimit(ServiceLimitInstanceType instanceType) {
        return serviceLimit.getInstanceTypes().stream().filter(s -> instanceType.equals(s.getInstanceType())).findFirst().get().getProcessorLimit();
    }

    private long fetchDefaultBridgeDuration(ServiceLimitInstanceType instanceType) {
        return serviceLimit.getInstanceTypes().stream().filter(s -> instanceType.equals(s.getInstanceType())).findFirst().get().getBridgeDuration();
    }

    private int fetchDefaultInstanceQuota(ServiceLimitInstanceType instanceType) {
        return serviceLimit.getInstanceTypes().stream().filter(s -> instanceType.equals(s.getInstanceType())).findFirst().get().getInstanceQuota();
    }
}
