package com.redhat.service.bridge.manager.models;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "FILTER")
public class Filter {

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, name = "key")
    private String key;

    @Column(nullable = false, name = "type")
    private String type;

    @Column(nullable = false, name = "value")
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_id")
    private Processor processor;

    public Filter() {
    }

    public Filter(String key, String type, String value, Processor processor) {
        this.key = key;
        this.type = type;
        this.value = value;
        this.processor = processor;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
