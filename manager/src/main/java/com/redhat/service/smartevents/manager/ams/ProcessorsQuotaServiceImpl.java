package com.redhat.service.smartevents.manager.ams;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class ProcessorsQuotaServiceImpl implements ProcessorsQuotaService {

    @Inject
    QuotaConfigurationProvider quotaConfigurationProvider;

    @Override
    public long getProcessorsQuota(String organisationId) {
        return quotaConfigurationProvider.getOrganisationQuotas(organisationId).getProcessorsQuota();
    }
}
