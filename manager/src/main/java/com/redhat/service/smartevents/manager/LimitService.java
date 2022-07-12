package com.redhat.service.smartevents.manager;

import com.redhat.service.smartevents.manager.models.QuotaLimit;

public interface LimitService {
    QuotaLimit getOrganisationQuotaLimit(String orgId);

    QuotaLimit getBridgeQuotaLimit(String bridgeId);
}
