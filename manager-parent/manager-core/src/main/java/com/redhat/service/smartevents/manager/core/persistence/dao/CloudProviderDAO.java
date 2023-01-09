package com.redhat.service.smartevents.manager.core.persistence.dao;

import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryPageInfo;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudProvider;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudRegion;

public interface CloudProviderDAO {

    ListResult<CloudRegion> listRegionsById(String cloudProviderId, QueryPageInfo queryPageInfo);

    ListResult<CloudProvider> list(QueryPageInfo queryInfo);

    CloudProvider findById(String cloudProviderId);
}
