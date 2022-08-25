package com.redhat.service.smartevents.manager.errorhandler;

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
import com.redhat.service.smartevents.manager.dao.ErrorDAO;
import com.redhat.service.smartevents.manager.models.ProcessingError;

import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;

@ApplicationScoped
public class ErrorHandlerProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlerProcessor.class);

    @Inject
    ErrorDAO errorDAO;
    @Inject
    ObjectMapper objectMapper;

    @Incoming("error-handler")
    public CompletionStage<Void> processError(final IncomingKafkaRecord<Integer, String> message) {
        try {
            Map<String, String> headers = parseHeaders(message.getHeaders());
            JsonNode payload = parsePayload(message.getPayload());

            ProcessingError processingError = new ProcessingError();
            processingError.setBridgeId(headers.get("rhose-bridge-id"));
            processingError.setRecordedAt(ZonedDateTime.now(ZoneOffset.UTC));
            processingError.setHeaders(headers);
            processingError.setPayload(payload);

            errorDAO.persist(processingError);

            LOGGER.debug("Persisted error {} for bridge {}", processingError.getId(), processingError.getBridgeId());
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
