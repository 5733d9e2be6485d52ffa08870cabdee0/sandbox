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
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ServiceLimitException;
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
    public QuotaLimit getOrganisationQuotaLimit(String orgId) {
        QuotaType availableQuotaType = getAvailableQuotaType(orgId);
        return getQuotaLimit(availableQuotaType);
    }

    @Override
    public QuotaLimit getBridgeQuotaLimit(String bridgeId) {
        Bridge bridge = bridgesService.getBridge(bridgeId);
        QuotaType bridgeInstanceType = bridge.getInstanceType();
        return getQuotaLimit(bridgeInstanceType);
    }

    private QuotaType getAvailableQuotaType(String orgId) {
        List<InstanceQuota> instanceQuotas = getOrganisationInstanceQuotas(orgId);

        if (isQuotaAvailable(orgId, instanceQuotas, QuotaType.STANDARD)) {
            return QuotaType.STANDARD;
        }

        if (isQuotaAvailable(orgId, instanceQuotas, QuotaType.EVAL)) {
            return QuotaType.EVAL;
        }

        throw new ServiceLimitException("Max allowed bridge instance limit exceed");
    }

    private boolean isQuotaAvailable(String orgId, List<InstanceQuota> instanceQuotas, QuotaType instanceType) {
        Optional<InstanceQuota> instanceQuota = instanceQuotas.stream().filter(s -> s.getInstanceType().equals(instanceType)).findAny();
        if (instanceQuota.isPresent()) {
            Long activeBridgeCount = bridgesService.getActiveBridgeCount(orgId, instanceType);
            long availableBridgeQuota = instanceQuota.get().getQuota() - activeBridgeCount;
            return availableBridgeQuota > 0;
        }
        return false;
    }

    private List<InstanceQuota> getOrganisationInstanceQuotas(String orgId) {
        Optional<OrganisationQuotas> optOrgOverride = fetchOrganisationOverride(orgId);
        if (optOrgOverride.isEmpty()) {
            return serviceLimit.getDefaultQuotas();
        } else {
            return optOrgOverride.get().getInstanceQuotas();
        }
    }

    private Optional<OrganisationQuotas> fetchOrganisationOverride(String orgId) {
        return serviceLimit.getOrganisationQuotas().stream().filter(s -> orgId.equals(s.getOrgId())).findAny();
    }

    private QuotaLimit getQuotaLimit(QuotaType quotaType) {
        return serviceLimit.getQuotaLimits().stream().filter(s -> s.getQuotaType().equals(quotaType)).findFirst()
                .orElseThrow(() -> new ServiceLimitException("No quota limit define for type " + quotaType));
    }
}
