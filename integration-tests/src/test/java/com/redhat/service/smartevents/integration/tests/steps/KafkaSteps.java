package com.redhat.service.smartevents.integration.tests.steps;

import java.util.List;

import org.assertj.core.api.Assertions;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.resources.kafka.KafkaResource;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

public class KafkaSteps {

    private TestContext context;

    public KafkaSteps(TestContext context) {
        this.context = context;
    }

    @And("^create a new Kafka topic \"([^\"]*)\"$")
    public void createKafkaTopic(String topicName) {
        String uniqueTopicName = context.getKafkaTopic(topicName);
        KafkaResource.createKafkaTopic(uniqueTopicName);
    }

    @And("^delete the Kafka topic \"([^\"]*)\"$")
    public void deleteKafkaTopic(String topicName) {
        String uniqueTopicName = context.getKafkaTopic(topicName);
        KafkaResource.deleteKafkaTopic(uniqueTopicName);
    }

    @Then("^Kafka topic \"([^\"]*)\" contains message \"([^\"]*)\"$")
    public void topicContainsString(String topicName, String message) {
        String uniqueTopicName = context.getKafkaTopic(topicName);
        List<String> data = KafkaResource.readTopic(uniqueTopicName);
        String uuidMessage = context.getUuid(message);
        Assertions.assertThat(data.stream().anyMatch(s -> s.contains(uuidMessage))).isTrue();
    }
}
