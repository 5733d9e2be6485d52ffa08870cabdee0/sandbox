package com.redhat.service.smartevents.manager.v2.persistence.dao;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import com.redhat.service.smartevents.manager.v2.persistence.models.Condition;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
@Transactional
public class ConditionDAO implements PanacheRepositoryBase<Condition, String> {
    // Empty atm
}
