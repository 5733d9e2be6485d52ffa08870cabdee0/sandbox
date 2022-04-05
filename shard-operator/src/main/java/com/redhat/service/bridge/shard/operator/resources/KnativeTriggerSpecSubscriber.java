package com.redhat.service.bridge.shard.operator.resources;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KnativeTriggerSpecSubscriber {
    private KnativeTriggerSpecSubscriberRef ref;
    private String uri;

    public KnativeTriggerSpecSubscriberRef getRef() {
        return ref;
    }

    public void setRef(KnativeTriggerSpecSubscriberRef ref) {
        this.ref = ref;
    }

    public String getUri() {
        return uri;
    }

    public void setIri(String iri) {
        this.uri = uri;
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
        return Objects.equals(ref, that.ref) && Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref, uri);
    }
}
