package com.redhat.service.bridge.rhoas;

import java.time.Duration;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.redhat.service.bridge.rhoas.dto.TopicAndServiceAccountRequest;
import com.redhat.service.bridge.rhoas.dto.TopicAndServiceAccountResponse;

/**
 * This dummy test API simulates a real usage of the {@link RhoasClient} in a real JAX-RS bean.
 * It allows {@link RhoasAPITest} to spot errors caused by the client mistakenly blocking the
 * Vert.x event loop thread, which can't be spotted testing directly the client in a QuarkusTest
 * (because blocking the test's main thread doesn't cause any error: it simply blocks).
 */
@Path("/rhoas/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RhoasAPI {

    @Inject
    Instance<RhoasClient> rhoasClient;

    @POST
    @Path("topic")
    public TopicAndServiceAccountResponse createTopicAndConsumerServiceAccount(TopicAndServiceAccountRequest request) {
        return rhoasClient.get().createTopicAndConsumerServiceAccount(request).await().atMost(Duration.ofSeconds(60));
    }

}
