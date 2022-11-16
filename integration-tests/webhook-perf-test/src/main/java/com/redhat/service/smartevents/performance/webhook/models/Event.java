package com.redhat.service.smartevents.performance.webhook.models;

import java.time.ZonedDateTime;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

@Entity
@Table(name = "events")
public class Event extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "eventIdSeq")
    @SequenceGenerator(name = "eventIdSeq", sequenceName = "EVENT_ID_SEQ")
    private Long id;

    @NotBlank
    @NotNull
    @Column(name = "bridgeId")
    private String bridgeId;

    @Column(name = "message", length = 65536) // Cloud event supports messages up to 64kB
    private String message;

    @NotBlank
    @NotNull
    @Column(name = "created_at")
    private ZonedDateTime submittedAt;

    @Column(name = "received_at")
    private ZonedDateTime receivedAt;

    public Long getId() {
        return id;
    }

    public Event setId(Long id) {
        this.id = id;
        return this;
    }

    public String getBridgeId() {
        return bridgeId;
    }

    public Event setBridgeId(String bridgeId) {
        this.bridgeId = bridgeId;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public Event setMessage(String message) {
        this.message = message;
        return this;
    }

    public ZonedDateTime getSubmittedAt() {
        return submittedAt;
    }

    public Event setSubmittedAt(ZonedDateTime submittedAt) {
        this.submittedAt = submittedAt;
        return this;
    }

    public ZonedDateTime getReceivedAt() {
        return receivedAt;
    }

    public Event setReceivedAt(ZonedDateTime receivedAt) {
        this.receivedAt = receivedAt;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Event event = (Event) o;
        return id.equals(event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Event{" +
                "bridgeId='" + bridgeId + '\'' +
                ", message='" + message + '\'' +
                ", submittedAt=" + submittedAt +
                ", receivedAt=" + receivedAt +
                '}';
    }
}