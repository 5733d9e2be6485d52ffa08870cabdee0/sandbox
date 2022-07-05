package com.redhat.service.smartevents.manager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ServiceLimitExceedException;
import com.redhat.service.smartevents.manager.config.ConfigurationLoader;
import com.redhat.service.smartevents.manager.limits.InstanceQuota;
import com.redhat.service.smartevents.manager.limits.OrganisationQuotas;
import com.redhat.service.smartevents.manager.limits.ServiceLimitConfig;
import com.redhat.service.smartevents.manager.models.*;

import io.quarkus.runtime.Startup;

@Startup
@ApplicationScoped
public class LimitServiceImpl implements LimitService {

    private static final String SERVICE_LIMIT_FILENAME = "service_limits.json";

    private ServiceLimitConfig serviceLimit;

    @Inject
    ObjectMapper mapper;

    @Inject
    ConfigurationLoader configurationLoader;

    @Inject
    BridgesService bridgesService;

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
    void validateServiceLimitJson(@Valid ServiceLimitConfig serviceLimit) {
    }

    @Override
    public Optional<InstanceLimit> getOrganisationInstanceLimit(String orgId) {
        LimitInstanceType availableInstanceQuotaType = getAvailableInstanceQuotaType(orgId);
        return getInstanceLimit(availableInstanceQuotaType);
    }

    @Override
    public Optional<InstanceLimit> getBridgeInstanceLimit(String bridgeId) {
        Bridge bridge = bridgesService.getBridge(bridgeId);
        LimitInstanceType bridgeInstanceType = bridge.getInstanceType();
        return getInstanceLimit(bridgeInstanceType);
    }

    private LimitInstanceType getAvailableInstanceQuotaType(String orgId) {
        List<InstanceQuota> instanceQuotas = getOrganisationInstanceQuotas(orgId);
        long activeBridgeCount = bridgesService.getActiveBridgeCount(orgId);
        long exceedBridgeCount = activeBridgeCount;
        Optional<InstanceQuota> standardInstanceQuota = instanceQuotas.stream().filter(s -> s.getInstanceType().equals(LimitInstanceType.STANDARD)).findAny();
        Optional<InstanceQuota> evalInstanceQuota = instanceQuotas.stream().filter(s -> s.getInstanceType().equals(LimitInstanceType.EVAL)).findAny();
        if (standardInstanceQuota.isPresent()) {
            exceedBridgeCount = activeBridgeCount - standardInstanceQuota.get().getQuota();
            if (exceedBridgeCount < 0) {
                return standardInstanceQuota.get().getInstanceType();
            }
        }

        if (evalInstanceQuota.isPresent()) {
            exceedBridgeCount = exceedBridgeCount - evalInstanceQuota.get().getQuota();
            if (exceedBridgeCount < 0) {
                return evalInstanceQuota.get().getInstanceType();
            }
        }

        throw new ServiceLimitExceedException("Max allowed bridge instance limit exceed");
    }

    private List<InstanceQuota> getOrganisationInstanceQuotas(String orgId) {
        Optional<OrganisationQuotas> optOrgOverride = fetchOrganisationOverride(orgId);
        if (optOrgOverride.isEmpty()) {
            return fetchDefaultQuota();
        } else {
            return optOrgOverride.get().getInstanceQuotas();
        }
    }

    private Optional<OrganisationQuotas> fetchOrganisationOverride(String orgId) {
        return serviceLimit.getOrganisationQuotas().stream().filter(s -> orgId.equals(s.getOrgId())).findAny();
    }

    private List<InstanceQuota> fetchDefaultQuota() {
        return serviceLimit.getDefaultQuotas();
    }

    private Optional<InstanceLimit> getInstanceLimit(LimitInstanceType instanceType) {
        return serviceLimit.getInstanceLimits().stream().filter(s -> s.getInstanceType().equals(instanceType)).findFirst();
    }

}
