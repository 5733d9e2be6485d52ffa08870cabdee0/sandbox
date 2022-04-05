package com.redhat.service.bridge.shard.operator.resources;

import java.net.URI;
import java.util.Objects;

public class KnativeTriggerSpecSubscriber {
    private KnativeTriggerSpecSubscriberRef ref;
    private URI iri;

    public KnativeTriggerSpecSubscriberRef getRef() {
        return ref;
    }

    public void setRef(KnativeTriggerSpecSubscriberRef ref) {
        this.ref = ref;
    }

    public URI getIri() {
        return iri;
    }

    public void setIri(URI iri) {
        this.iri = iri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KnativeTriggerSpecSubscriber that = (KnativeTriggerSpecSubscriber) o;
        return Objects.equals(ref, that.ref) && Objects.equals(iri, that.iri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref, iri);
    }
}
