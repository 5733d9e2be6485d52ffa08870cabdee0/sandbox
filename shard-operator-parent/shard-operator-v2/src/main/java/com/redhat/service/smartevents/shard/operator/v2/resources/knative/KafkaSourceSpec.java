package com.redhat.service.smartevents.shard.operator.v2.resources.knative;

import java.util.List;
import java.util.Objects;

public class KafkaSourceSpec {

    int consumers;

    List<String> bootstrapServers;

    KafkaNetSpec net;

    List<String> topics;

    String consumerGroup;

    String initialOffset;

    Destination sink;

    public int getConsumers() {
        return consumers;
    }

    public void setConsumers(int consumers) {
        this.consumers = consumers;
    }

    public List<String> getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(List<String> bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public KafkaNetSpec getNet() {
        return net;
    }

    public void setNet(KafkaNetSpec net) {
        this.net = net;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getInitialOffset() {
        return initialOffset;
    }

    public void setInitialOffset(String initialOffset) {
        this.initialOffset = initialOffset;
    }

    public Destination getSink() {
        return sink;
    }

    public void setSink(Destination sink) {
        this.sink = sink;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        KafkaSourceSpec that = (KafkaSourceSpec) o;
        return consumers == that.consumers && Objects.equals(bootstrapServers, that.bootstrapServers) && Objects.equals(net, that.net) && Objects.equals(topics, that.topics)
                && Objects.equals(consumerGroup, that.consumerGroup) && Objects.equals(initialOffset, that.initialOffset) && Objects.equals(sink, that.sink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consumers, bootstrapServers, net, topics, consumerGroup, initialOffset, sink);
    }
}
