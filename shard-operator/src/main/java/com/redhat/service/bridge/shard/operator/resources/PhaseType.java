package com.redhat.service.bridge.shard.operator.resources;

public enum PhaseType {
    INITIALIZATION("INITIALIZATION"),
    AUGMENTATION("AUGMENTATION"),
    AVAILABLE("AVAILABLE"),
    ERROR("ERROR");

    PhaseType(String initialization) {
    }
}