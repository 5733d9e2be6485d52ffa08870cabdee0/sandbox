package com.redhat.service.smartevents.infra.exceptions;

import java.util.Objects;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema
public class BridgeErrorInstance extends BridgeError {

    @JsonProperty
    private String uuid;

    protected BridgeErrorInstance() {
        //(De-)serialisation
    }

    public BridgeErrorInstance(BridgeError bridgeError) {
        this(bridgeError, UUID.randomUUID().toString());
    }

    public BridgeErrorInstance(BridgeError bridgeError, String uuid) {
        super(bridgeError.getId(), bridgeError.getCode(), bridgeError.getReason(), bridgeError.getType());
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BridgeErrorInstance)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        BridgeErrorInstance that = (BridgeErrorInstance) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uuid);
    }

    @Override
    public String toString() {
        return "BridgeErrorInstance{" +
                "bridgeError=" + super.toString() +
                ", uuid='" + uuid + '\'' +
                '}';
    }
}
