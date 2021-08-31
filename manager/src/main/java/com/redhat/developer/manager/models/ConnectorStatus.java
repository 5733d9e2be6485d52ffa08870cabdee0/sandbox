package com.redhat.developer.manager.models;

import com.redhat.developer.infra.dto.ConnectorStatusDTO;

public enum ConnectorStatus {
    REQUESTED("REQUESTED"),
    PROVISIONING("PROVISIONING"),
    AVAILABLE("AVAILABLE");

    private String status;

    ConnectorStatus(String status) {
        this.status = status;
    }

    public ConnectorStatusDTO toDTO() {
        if (this.equals(REQUESTED)) {
            return ConnectorStatusDTO.REQUESTED;
        }
        if (this.equals(PROVISIONING)) {
            return ConnectorStatusDTO.PROVISIONING;
        }
        if (this.equals(AVAILABLE)) {
            return ConnectorStatusDTO.AVAILABLE;
        }
        throw new RuntimeException("Not supported ConnectorStatus");
    }

    public static ConnectorStatus fromDTO(ConnectorStatusDTO dto) {
        if (dto.equals(ConnectorStatusDTO.REQUESTED)) {
            return REQUESTED;
        }
        if (dto.equals(ConnectorStatusDTO.PROVISIONING)) {
            return PROVISIONING;
        }
        if (dto.equals(ConnectorStatusDTO.AVAILABLE)) {
            return AVAILABLE;
        }
        throw new RuntimeException("Not supported ConnectorStatus");
    }
}
