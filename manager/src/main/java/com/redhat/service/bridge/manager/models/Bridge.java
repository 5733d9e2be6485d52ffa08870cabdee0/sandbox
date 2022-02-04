package com.redhat.service.bridge.manager.models;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import com.redhat.service.bridge.infra.models.dto.BridgeStatus;
import com.redhat.service.bridge.manager.config.ConfigUtils;

@NamedQueries({
        @NamedQuery(name = "BRIDGE.findByStatuses",
                query = "from Bridge where status IN :statuses"),
        @NamedQuery(name = "BRIDGE.findByNameAndCustomerId",
                query = "from Bridge where name=:name and customer_id=:customerId"),
        @NamedQuery(name = "BRIDGE.findByIdAndCustomerId",
                query = "from Bridge where id=:id and customer_id=:customerId"),
        @NamedQuery(name = "BRIDGE.findByCustomerId",
                query = "from Bridge where customer_id=:customerId order by submitted_at desc"),
})
@Entity
@Table(name = "BRIDGE", uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "customer_id" }) })
public class Bridge {

    public static final String CUSTOMER_ID_PARAM = "customerId";
    public static final String TOPIC_PREFIX = ConfigUtils.topicPrefix();

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(name = "name", nullable = false, updatable = false)
    private String name;

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private String customerId;

    @Column(name = "submitted_at", updatable = false, nullable = false, columnDefinition = "TIMESTAMP")
    private ZonedDateTime submittedAt;

    @Column(name = "published_at", columnDefinition = "TIMESTAMP")
    private ZonedDateTime publishedAt;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private BridgeStatus status;

    public Bridge() {
    }

    public Bridge(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getCustomerId() {
        return customerId;
    }

    public ZonedDateTime getPublishedAt() {
        return publishedAt;
    }

    public ZonedDateTime getSubmittedAt() {
        return submittedAt;
    }

    public BridgeStatus getStatus() {
        return status;
    }

    @Transient
    public String getTopicName() {
        return TOPIC_PREFIX + getId();
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setPublishedAt(ZonedDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setSubmittedAt(ZonedDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public void setStatus(BridgeStatus status) {
        this.status = status;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /*
     * See: https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
     * In the context of JPA equality, our id is our unique business key as we generate it via UUID.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Bridge bridge = (Bridge) o;
        return id.equals(bridge.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
