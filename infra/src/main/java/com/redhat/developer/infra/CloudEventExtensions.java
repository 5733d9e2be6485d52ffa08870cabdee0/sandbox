package com.redhat.developer.infra;

public class CloudEventExtensions {

    /**
     * An extension attribute added to incoming events to identify the target bridge instance
     */
    public static final String BRIDGE_ID_EXTENSION = "ebbridgeid";

    private CloudEventExtensions() {

    }
}
