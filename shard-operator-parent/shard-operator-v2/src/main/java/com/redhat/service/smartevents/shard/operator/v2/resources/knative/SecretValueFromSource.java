package com.redhat.service.smartevents.shard.operator.v2.resources.knative;

import java.util.Objects;

import io.fabric8.kubernetes.api.model.SecretKeySelector;

public class SecretValueFromSource {

    SecretKeySelector secretKeyRef;

    public SecretKeySelector getSecretKeyRef() {
        return secretKeyRef;
    }

    public void setSecretKeyRef(SecretKeySelector secretKeyRef) {
        this.secretKeyRef = secretKeyRef;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SecretValueFromSource that = (SecretValueFromSource) o;
        return Objects.equals(secretKeyRef, that.secretKeyRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(secretKeyRef);
    }
}
