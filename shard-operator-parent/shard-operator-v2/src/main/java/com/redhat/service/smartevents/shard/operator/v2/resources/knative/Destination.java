package com.redhat.service.smartevents.shard.operator.v2.resources.knative;

import java.util.Objects;

public class Destination {

    KReference ref;

    String uri;

    public KReference getRef() {
        return ref;
    }

    public void setRef(KReference ref) {
        this.ref = ref;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Destination that = (Destination) o;
        return Objects.equals(ref, that.ref) && Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ref, uri);
    }
}
