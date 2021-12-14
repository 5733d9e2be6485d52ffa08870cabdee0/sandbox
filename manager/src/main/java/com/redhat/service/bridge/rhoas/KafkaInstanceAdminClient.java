package com.redhat.service.bridge.rhoas;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import com.redhat.service.bridge.rhoas.auth.MasSSOHeaderFactory;
import com.redhat.service.bridge.rhoas.dto.Topics;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@RegisterClientHeaders(MasSSOHeaderFactory.class)
public interface KafkaInstanceAdminClient {

    @GET
    @Path("/topics")
    Topics listTopics();

}
