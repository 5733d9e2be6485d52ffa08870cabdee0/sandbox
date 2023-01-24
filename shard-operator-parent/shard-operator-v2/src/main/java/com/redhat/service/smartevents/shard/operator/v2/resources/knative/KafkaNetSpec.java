package com.redhat.service.smartevents.shard.operator.v2.resources.knative;

import java.util.Objects;

public class KafkaNetSpec {

    KafkaSASLSpec sasl;

    KafkaTLSSpec tls;

    public KafkaSASLSpec getSasl() {
        return sasl;
    }

    public void setSasl(KafkaSASLSpec sasl) {
        this.sasl = sasl;
    }

    public KafkaTLSSpec getTls() {
        return tls;
    }

    public void setTls(KafkaTLSSpec tls) {
        this.tls = tls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        KafkaNetSpec that = (KafkaNetSpec) o;
        return Objects.equals(sasl, that.sasl) && Objects.equals(tls, that.tls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sasl, tls);
    }
}
