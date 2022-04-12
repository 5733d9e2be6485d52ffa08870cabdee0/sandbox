package com.redhat.service.rhose.manager.models;

import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@NamedQueries({
        @NamedQuery(name = "SHARD.findByType",
                query = "from Shard where type=:type")
})
@Entity
public class Shard {

    public static final String ID_PARAM = "id";

    public static final String TYPE_PARAM = "type";

    @Id
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, name = "type")
    @Enumerated(EnumType.STRING)
    private ShardType type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ShardType getType() {
        return type;
    }

    public void setType(ShardType type) {
        this.type = type;
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
        Shard shard = (Shard) o;
        return id.equals(shard.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
