package com.redhat.service.smartevents.manager.dao;

import java.util.Objects;
import java.util.Properties;

import org.hibernate.annotations.FilterDef;
import org.hibernate.type.EnumType;
import org.hibernate.usertype.UserType;

/**
 * If we define the type of the applicable {@link FilterDef} as the enum class itself Hibernate uses Java
 * serialization to a PostgreSQL bytea type as the value in Filters. This does not work since we choose to explicitly
 * store Enums as {@link javax.persistence.EnumType#STRING}. This class therefore represents a custom type usable in Filters.
 */
public abstract class EnumTypeBase<T extends Enum> extends EnumType<T> {

    /**
     * There appears to be a bug in either {@link EnumType} or {@link org.hibernate.type.TypeFactory}.
     * When a {@link FilterDef} is defined to use a {@link UserType} the properties to allow the {@link EnumType} to be
     * correctly instantiated are not provided and creation fails. This works around the issue.
     */
    @Override
    public void setParameterValues(Properties parameters) {
        Properties clone = new Properties();
        if (Objects.nonNull(parameters)) {
            clone.putAll(parameters);
        }

        if (!clone.containsKey(EnumType.ENUM)) {
            clone.setProperty(EnumType.ENUM, getEnumClass().getName());
        }
        if (!clone.containsKey(EnumType.NAMED)) {
            // Hard-coded as we know we use STRING's and not ORDINAL's.
            clone.setProperty(EnumType.NAMED, "true");
        }
        super.setParameterValues(clone);
    }

    protected abstract Class<T> getEnumClass();
}
