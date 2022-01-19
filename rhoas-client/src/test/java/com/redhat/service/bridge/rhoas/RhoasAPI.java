package com.redhat.service.bridge.rhoas;

import java.time.Duration;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.Topic;
import com.openshift.cloud.api.kas.auth.models.TopicSettings;

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
    public Topic createTopicAndConsumerServiceAccount(Request request) {
        return rhoasClient.get()
                .createTopicAndGrantAccess(
                        new NewTopicInput().name(request.topicName).settings(new TopicSettings().numPartitions(1)),
                        request.serviceAccountId,
                        RhoasTopicAccessType.CONSUMER)
                .await().atMost(Duration.ofSeconds(60));
    }

    public static class Request {
        public String topicName;
        public String serviceAccountId;

        public Request() {
        }

        public Request(String topicName, String serviceAccountId) {
            this.topicName = topicName;
            this.serviceAccountId = serviceAccountId;
        }
    }
}
