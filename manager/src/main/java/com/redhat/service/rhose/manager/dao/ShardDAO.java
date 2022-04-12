package com.redhat.service.rhose.manager.dao;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.rhose.manager.models.Shard;
import com.redhat.service.rhose.manager.models.ShardType;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@ApplicationScoped
@Transactional
public class ShardDAO implements PanacheRepositoryBase<Shard, String> {

    public List<Shard> findByType(ShardType type) {
        Parameters p = Parameters.with(Shard.TYPE_PARAM, type);
        return find("#SHARD.findByType", p).list();
    }
}
