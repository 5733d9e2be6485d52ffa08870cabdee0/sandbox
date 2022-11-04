package com.redhat.service.smartevents.manager.persistence.v1.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.manager.persistence.v1.models.Shard;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
@Transactional
public class ShardDAO implements PanacheRepositoryBase<Shard, String> {
}
