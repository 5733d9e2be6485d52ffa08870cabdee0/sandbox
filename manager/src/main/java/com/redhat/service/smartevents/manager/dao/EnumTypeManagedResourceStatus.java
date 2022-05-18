package com.redhat.service.smartevents.manager.dao;

import java.util.Objects;
import java.util.Properties;

import org.hibernate.annotations.FilterDef;
import org.hibernate.type.EnumType;
import org.hibernate.usertype.UserType;

import com.redhat.service.smartevents.infra.models.dto.ManagedResourceStatus;

/**
 * Custom Type for ManagedResourceStatus enumeration.
 * If we define the type of the applicable {@link FilterDef} as the enum class itself Hibernate uses Java
 * serialization to a PostgreSQL bytea type as the value in Filters. This does not work since we choose to explicitly
 * store Enums as {@link javax.persistence.EnumType#STRING}. This class therefore represents a custom type usable in Filters.
 */
public class EnumTypeManagedResourceStatus extends EnumType<ManagedResourceStatus> {

    @Override
    /**
     * There appears to be a bug in either {@link org.hibernate.type.EnumType} or {@link org.hibernate.type.TypeFactory}.
     * When a {@link FilterDef} is defined to use a {@link UserType} the properties to allow the {@link EnumType} to be
     * correctly instantiated are not provided and creation fails. This works around the issue.
     */
    public void setParameterValues(Properties parameters) {
        if (Objects.isNull(parameters)) {
            parameters = new Properties();
        }
        if (!parameters.containsKey(EnumType.ENUM)) {
            parameters.setProperty(EnumType.ENUM, ManagedResourceStatus.class.getName());
        }
        if (!parameters.containsKey(EnumType.NAMED)) {
            // Hard-coded as we know we use STRING's and not ORDINAL's.
            parameters.setProperty(EnumType.NAMED, "true");
        }
        super.setParameterValues(parameters);
    }
}
