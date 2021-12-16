package com.redhat.service.bridge.manager;

import java.time.Duration;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.manager.models.TopicAndServiceAccount;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;

@QuarkusTest
class RhoasServiceTest {

    @Inject
    RhoasService service;

    @Test
    void testCreateTopicAndServiceAccount() {
        TopicAndServiceAccount tasa = service.createTopicAndServiceAccount("it-topic-1", "alcosta-mgdobr-168--topic--it-topic-1--ro")
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem(Duration.ofSeconds(60))
                .getItem();
        System.out.println(tasa.toString());
    }
}
