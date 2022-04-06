package com.redhat.service.bridge.executor.api;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.executor.Executor;
import com.redhat.service.bridge.executor.ExecutorsProvider;
import com.redhat.service.bridge.executor.ExecutorsService;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.v1.CloudEventBuilder;

@Path("/events")
public class ExecutorAPI {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorsService.class);

    @Inject
    ExecutorsProvider executorsProvider;

    @POST
    @Produces("*/*")
    @Consumes("*/*")
    public Response consumeEvent(@Context HttpHeaders headers, String event) {
        LOG.info("Received event on endpoint /events");
        Executor executor = executorsProvider.getExecutor();
        try {
            Map<String, String> ceheaders = new HashMap<>();
            for (String h : headers.getRequestHeaders().keySet()) {
                ceheaders.put(h.substring(3), headers.getHeaderString(h));
            }

            CloudEvent ce = new CloudEventBuilder()
                    .withData(event.getBytes(StandardCharsets.UTF_8))
                    .withId(ceheaders.get("id"))
                    .withSource(new URI(ceheaders.get("source")))
                    .withType(ceheaders.get("type"))
                    .withDataSchema(new URI(ceheaders.get("dataschema")))
                    .withSubject(ceheaders.get("subject"))
                    .build();
            executor.onEvent(ce);
        } catch (Throwable t) {
            // Inner Throwable catch is to provide more specific context around which Executor failed to handle the Event, rather than a generic failure
            LOG.error("Processor with id '{}' on bridge '{}' failed to handle Event.", executor.getProcessor().getId(),
                    executor.getProcessor().getBridgeId(), t);
            return Response.status(500).build();
        }
        return Response.ok().build();
    }
}