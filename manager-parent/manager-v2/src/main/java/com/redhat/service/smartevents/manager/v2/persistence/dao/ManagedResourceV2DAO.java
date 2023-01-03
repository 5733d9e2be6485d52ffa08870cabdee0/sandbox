package com.redhat.service.smartevents.manager.v2.persistence.dao;

import java.util.List;

import com.redhat.service.smartevents.manager.v2.persistence.models.ManagedResourceV2;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

public interface ManagedResourceV2DAO<T extends ManagedResourceV2> extends PanacheRepositoryBase<T, String> {

    T findByIdWithConditions(String id);

    List<T> findByShardIdToDeployOrDelete(String shardId);
}
