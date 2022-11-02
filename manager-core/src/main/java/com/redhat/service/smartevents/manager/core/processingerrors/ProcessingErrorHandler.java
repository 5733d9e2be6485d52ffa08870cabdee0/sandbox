package com.redhat.service.smartevents.manager.core.processingerrors;

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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.manager.core.persistence.dao.ProcessingErrorDAO;
import com.redhat.service.smartevents.manager.core.persistence.models.ProcessingError;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;

import static com.redhat.service.smartevents.infra.core.api.APIConstants.RHOSE_BRIDGE_ID_HEADER;

@ApplicationScoped
public class ProcessingErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingErrorHandler.class);

    @ConfigProperty(name = "event-bridge.processing-errors.max-errors-per-bridge")
    int maxErrorsPerBridge;

    @Inject
    ProcessingErrorDAO processingErrorDAO;

    @Inject
    ObjectMapper objectMapper;

    @Incoming("processing-errors")
    @Blocking
    public CompletionStage<Void> processError(final IncomingKafkaRecord<Integer, String> message) {
        try {
            Map<String, String> headers = parseHeaders(message.getHeaders());
            JsonNode payload = parsePayload(message.getPayload());

            String bridgeId = headers.get(RHOSE_BRIDGE_ID_HEADER);
            if (bridgeId != null && !bridgeId.isBlank()) {
                ProcessingError processingError = new ProcessingError();
                processingError.setBridgeId(bridgeId);
                processingError.setRecordedAt(ZonedDateTime.now(ZoneOffset.UTC));
                processingError.setHeaders(headers);
                processingError.setPayload(payload);

                processingErrorDAO.persist(processingError);

                LOGGER.debug("Persisted error {} for bridge {}", processingError.getId(), processingError.getBridgeId());
            } else {
                LOGGER.error("Received message without bridge ID. Message acked anyway.\nheaders = {}\npayload = {}\n",
                        message.getHeaders(), message.getPayload());
            }
        } catch (Exception e) {
            LOGGER.error("Error when deserializing error message. Message acked anyway.\nheaders = {}\npayload = {}\n",
                    message.getHeaders(), message.getPayload(), e);
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
        return payload == null ? null : objectMapper.readTree(payload);
    }

    @Scheduled(cron = "{event-bridge.processing-errors.cleanup.schedule}")
    void cleanup() {
        LOGGER.debug("Processing errors cleanup triggered");
        processingErrorDAO.cleanup(maxErrorsPerBridge);
    }
}
