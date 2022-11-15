package com.redhat.service.smartevents.performance.webhook.models;

import java.time.ZonedDateTime;

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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Entity
@Table(name = "events")
@EqualsAndHashCode(callSuper = false)
@ToString
@Accessors(chain = true)
@Getter
@Setter
public class Event extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "eventIdSeq")
    @SequenceGenerator(name = "eventIdSeq", sequenceName = "EVENT_ID_SEQ")
    @EqualsAndHashCode.Exclude
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

    public Event copy(Event plan) {
        this.bridgeId = plan.bridgeId;
        this.message = plan.message;
        this.submittedAt = plan.submittedAt;
        this.receivedAt = plan.receivedAt;
        return this;
    }
}