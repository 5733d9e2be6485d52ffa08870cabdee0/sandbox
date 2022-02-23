package com.redhat.service.bridge.manager.dao;

import java.util.List;

import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.models.dto.ManagedEntityStatus;
import com.redhat.service.bridge.manager.models.ManagedEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

public interface ManagedEntityDAO<T extends ManagedEntity> extends PanacheRepositoryBase<T, String> {
    // Empty atm, but this will contain the common methods across the managed entities
}
