package com.redhat.service.smartevents.manager;

import com.redhat.service.smartevents.manager.models.OrganisationServiceLimit;

public interface LimitService {
    OrganisationServiceLimit getOrganisationServiceLimit(String orgId);
}
