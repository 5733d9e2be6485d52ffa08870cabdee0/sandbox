package com.redhat.service.smartevents.manager;

import java.util.Optional;

import com.redhat.service.smartevents.manager.models.InstanceLimit;

public interface LimitService {
    Optional<InstanceLimit> getOrganisationInstanceLimit(String orgId);

    Optional<InstanceLimit> getBridgeInstanceLimit(String bridgeId);
}
