package com.redhat.service.smartevents.manager;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.manager.dao.ErrorDAO;
import com.redhat.service.smartevents.manager.models.Error;
import com.redhat.service.smartevents.manager.providers.InternalKafkaConfigurationProvider;
import com.redhat.service.smartevents.manager.providers.ResourceNamesProvider;

import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;

@ApplicationScoped
public class ErrorHandlerServiceImpl implements ErrorHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlerServiceImpl.class);

    @Inject
    InternalKafkaConfigurationProvider internalKafkaConfigurationProvider;
    @Inject
    ResourceNamesProvider resourceNamesProvider;
    @Inject
    ObjectMapper objectMapper;
    @Inject
    ErrorDAO errorDAO;

    @Override
    public Action getDefaultErrorHandlerAction() {
        Action action = new Action();
        action.setType("kafka_topic_sink_0.1");
        action.setMapParameters(Map.of(
                "topic", resourceNamesProvider.getGlobalErrorTopicName(),
                "kafka_broker_url", internalKafkaConfigurationProvider.getBootstrapServers(),
                "kafka_client_id", internalKafkaConfigurationProvider.getClientId(),
                "kafka_client_secret", internalKafkaConfigurationProvider.getClientSecret()));
        return action;
    }

    @Incoming("errors")
    public CompletionStage<Void> processError(final IncomingKafkaRecord<Integer, String> message) {
        try {
            Map<String, String> headers = parseHeaders(message.getHeaders());
            JsonNode payload = parsePayload(message.getPayload());

            Error error = new Error();
            error.setBridgeId(headers.get("rhose-bridge-id"));
            error.setRecordedAt(ZonedDateTime.now(ZoneOffset.UTC));
            error.setHeaders(headers);
            error.setPayload(payload);

            errorDAO.persist(error);

            LOGGER.debug("Persisted error {} for bridge {}", error.getId(), error.getBridgeId());
        } catch (Exception e) {
            LOGGER.error("Error when deserializing error message {}. Message acked anyway.", message, e);
        }
        return message.ack();
    }

    private Map<String, String> parseHeaders(Headers headers) {
        Map<String, String> headersMap = new TreeMap<>();
        if (headers != null) {
            for (Header header : headers) {
                headersMap.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
            }
        }
        return headersMap;
    }

    private JsonNode parsePayload(String payload) throws JsonProcessingException {
        return objectMapper.readTree(payload);
    }
}
