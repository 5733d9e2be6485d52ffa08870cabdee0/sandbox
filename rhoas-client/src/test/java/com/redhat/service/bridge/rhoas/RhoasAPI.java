package com.redhat.service.bridge.rhoas;

import java.time.Duration;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.redhat.service.bridge.rhoas.dto.TopicAndServiceAccountRequest;
import com.redhat.service.bridge.rhoas.dto.TopicAndServiceAccountResponse;

@Path("/rhoas/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RhoasAPI {

    @Inject
    RhoasClient rhoasService;

    @POST
    @Path("topic")
    public TopicAndServiceAccountResponse createTopicAndConsumerServiceAccount(TopicAndServiceAccountRequest request) {
        return rhoasService.createTopicAndConsumerServiceAccount(request).await().atMost(Duration.ofSeconds(60));
    }

}
