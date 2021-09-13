package com.redhat.service.bridge.infra;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import io.cloudevents.CloudEventExtension;
import io.cloudevents.CloudEventExtensions;
import io.cloudevents.core.extensions.impl.ExtensionUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

public class BridgeCloudEventExtension implements CloudEventExtension {

    /**
     * An extension attribute added to incoming events to identify the target bridge instance
     */
    public static final String BRIDGE_ID = "ebbridgeid";

    private static final Set<String> KEYS = unmodifiableSet(new HashSet<>(asList(BRIDGE_ID)));

    private static void readStringExtension(CloudEventExtensions extensions, String key, Consumer<String> consumer) {
        Optional.ofNullable(extensions.getExtension(key))
                // there seems to be a bug in the cloudevents sdk so that, when a extension attributes is null,
                // it returns a "null" String instead of a real null object
                .filter(obj -> !("null".equals(obj)))
                .map(Object::toString)
                .ifPresent(consumer);
    }

    private String bridgeId;

    public BridgeCloudEventExtension() {

    }

    public BridgeCloudEventExtension(String bridgeId) {
        this.bridgeId = bridgeId;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public void setBridgeId(String bridgeId) {
        this.bridgeId = bridgeId;
    }

    @Override
    public void readFrom(CloudEventExtensions extensions) {
        readStringExtension(extensions, BRIDGE_ID, this::setBridgeId);
    }

    @Override
    public Object getValue(String key) throws IllegalArgumentException {
        switch (key) {
            case BRIDGE_ID:
                return this.getBridgeId();
            default:
                throw ExtensionUtils.generateInvalidKeyException(this.getClass(), key);
        }
    }

    @Override
    public Set<String> getKeys() {
        return KEYS;
    }
}
