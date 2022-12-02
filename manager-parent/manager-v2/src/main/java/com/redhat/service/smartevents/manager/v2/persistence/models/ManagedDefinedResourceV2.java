package com.redhat.service.smartevents.manager.v2.persistence.models;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import io.quarkiverse.hibernate.types.json.JsonBinaryType;
import io.quarkiverse.hibernate.types.json.JsonTypes;

@MappedSuperclass
@TypeDef(name = JsonTypes.JSON_BIN, typeClass = JsonBinaryType.class)
public abstract class ManagedDefinedResourceV2<T> extends ManagedResourceV2 {

    @Type(type = JsonTypes.JSON_BIN)
    @Column(name = "definition", columnDefinition = JsonTypes.JSON_BIN)
    protected T definition;

    public T getDefinition() {
        return definition;
    }

    public void setDefinition(T definition) {
        this.definition = definition;
    }

}
