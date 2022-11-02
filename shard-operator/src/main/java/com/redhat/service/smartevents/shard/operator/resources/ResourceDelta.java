package com.redhat.service.smartevents.shard.operator.resources;

import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.ArrayList;
import java.util.List;

public class ResourceDelta<T extends HasMetadata> {

    List<T> created;
    List<T> updated;
    List<T> deleted;

    public ResourceDelta() {
        created = new ArrayList<>();
        updated = new ArrayList<>();
        deleted = new ArrayList<>();
    }

    public List<T> getCreated() {
        return created;
    }

    public void setCreated(List<T> created) {
        this.created = created;
    }

    public List<T> getUpdated() {
        return updated;
    }

    public void setUpdated(List<T> updated) {
        this.updated = updated;
    }

    public List<T> getDeleted() {
        return deleted;
    }

    public void setDeleted(List<T> deleted) {
        this.deleted = deleted;
    }

    public boolean HasChanged() {
        if (!created.isEmpty()) {
            return true;
        }
        if (!updated.isEmpty()) {
            return true;
        }
        if (!deleted.isEmpty()) {
            return true;
        }
        return false;
    }
}
