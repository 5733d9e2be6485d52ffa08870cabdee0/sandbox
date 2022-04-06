package com.redhat.service.bridge.shard.operator.resources;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KnativeTriggerSpec {
    private String broker;

    private KnativeTriggerSpecFilter filter;

    private KnativeTriggerSpecSubscriber subscriber;

    public String getBroker() {
        return broker;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public KnativeTriggerSpecFilter getFilter() {
        return filter;
    }

    public void setFilter(KnativeTriggerSpecFilter filter) {
        this.filter = filter;
    }

    public KnativeTriggerSpecSubscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(KnativeTriggerSpecSubscriber subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KnativeTriggerSpec that = (KnativeTriggerSpec) o;
        return Objects.equals(broker, that.broker) && Objects.equals(filter, that.filter) && Objects.equals(subscriber, that.subscriber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(broker, filter, subscriber);
    }
}
