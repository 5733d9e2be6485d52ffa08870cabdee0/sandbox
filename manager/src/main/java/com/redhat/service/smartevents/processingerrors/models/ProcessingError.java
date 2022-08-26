package com.redhat.service.smartevents.processingerrors.models;

import java.time.ZonedDateTime;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkiverse.hibernate.types.json.JsonTypes;

@NamedQuery(name = "PROCESSING_ERROR.findByBridgeIdOrdered",
        query = "from ProcessingError where bridge_id=:bridgeId order by id desc")
@Entity
@Table(name = "PROCESSING_ERROR")
public class ProcessingError {

    @Id
    @SequenceGenerator(name = "errorId", sequenceName = "PROCESSING_ERROR_ID_SEQUENCE", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "errorId")
    private Long id;

    @Column(name = "bridge_id", nullable = false, updatable = false)
    private String bridgeId;

    @Column(name = "recorded_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private ZonedDateTime recordedAt;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(name = "headers", nullable = false, updatable = false, columnDefinition = JsonTypes.JSON_BIN)
    private Map<String, String> headers;

    @Type(type = JsonTypes.JSON_BIN)
    @Column(name = "payload", nullable = false, updatable = false, columnDefinition = JsonTypes.JSON_BIN)
    private JsonNode payload;

    public Long getId() {
        return id;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public void setBridgeId(String bridgeId) {
        this.bridgeId = bridgeId;
    }

    public ZonedDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(ZonedDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}
