package com.redhat.service.smartevents.shard.operator.v2.resources.knative;

import java.util.Objects;

public class KafkaTLSSpec {

    boolean enable;

    SecretValueFromSource cert;

    SecretValueFromSource key;

    SecretValueFromSource caCert;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public SecretValueFromSource getCert() {
        return cert;
    }

    public void setCert(SecretValueFromSource cert) {
        this.cert = cert;
    }

    public SecretValueFromSource getKey() {
        return key;
    }

    public void setKey(SecretValueFromSource key) {
        this.key = key;
    }

    public SecretValueFromSource getCaCert() {
        return caCert;
    }

    public void setCaCert(SecretValueFromSource caCert) {
        this.caCert = caCert;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        KafkaTLSSpec that = (KafkaTLSSpec) o;
        return enable == that.enable && Objects.equals(cert, that.cert) && Objects.equals(key, that.key) && Objects.equals(caCert, that.caCert);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enable, cert, key, caCert);
    }
}
