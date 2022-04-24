package com.redhat.service.smartevents.manager.models;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Type;

import io.quarkiverse.hibernate.types.json.JsonTypes;

@MappedSuperclass
public class ManagedDefinedResource<T> extends ManagedResource {

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
