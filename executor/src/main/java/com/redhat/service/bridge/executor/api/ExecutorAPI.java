package com.redhat.service.bridge.executor.api;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.executor.Executor;
import com.redhat.service.bridge.executor.ExecutorsProvider;
import com.redhat.service.bridge.executor.ExecutorsService;

import io.cloudevents.CloudEvent;

@Path("/events")
public class ExecutorAPI {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorsService.class);

    @Inject
    ExecutorsProvider executorsProvider;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response consumeEvent(@NotNull CloudEvent event) {
        Executor executor = executorsProvider.getExecutor();
        try {
            executor.onEvent(event);
        } catch (Throwable t) {
            // Inner Throwable catch is to provide more specific context around which Executor failed to handle the Event, rather than a generic failure
            LOG.error("Processor with id '{}' on bridge '{}' failed to handle Event.", executor.getProcessor().getId(),
                    executor.getProcessor().getBridgeId(), t);
            return Response.status(500).build();
        }
        return Response.ok().build();
    }
}
