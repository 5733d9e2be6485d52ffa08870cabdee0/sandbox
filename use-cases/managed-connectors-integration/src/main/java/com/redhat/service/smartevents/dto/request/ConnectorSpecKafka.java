
package com.redhat.service.smartevents.dto.request;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * Managed Kafka Source
 * <p>
 *
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "topic"
})
@Generated("jsonschema2pojo")
public class ConnectorSpecKafka {

    @JsonProperty("bootstrap_server")
    String bootstrapServer;

    /**
     * Topic Names
     * <p>
     * Comma separated list of Kafka topic names
     * (Required)
     *
     */
    @JsonProperty("topic")
    @JsonPropertyDescription("Comma separated list of Kafka topic names")
    private String topic;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * Topic Names
     * <p>
     * Comma separated list of Kafka topic names
     * (Required)
     *
     */
    @JsonProperty("topic")
    public String getTopic() {
        return topic;
    }

    /**
     * Topic Names
     * <p>
     * Comma separated list of Kafka topic names
     * (Required)
     *
     */
    @JsonProperty("topic")
    public void setTopic(String topic) {
        this.topic = topic;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        return "ConnectorSpecKafka{" +
                "bootstrapServer='" + bootstrapServer + '\'' +
                ", topic='" + topic + '\'' +
                ", additionalProperties=" + additionalProperties +
                '}';
    }
}
