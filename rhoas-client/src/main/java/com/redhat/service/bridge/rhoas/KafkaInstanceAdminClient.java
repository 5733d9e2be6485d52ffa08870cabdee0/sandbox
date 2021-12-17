package com.redhat.service.bridge.rhoas;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.service.bridge.rhoas.auth.MasSSOHeaderFactory;
import com.redhat.service.bridge.rhoas.dto.AclBinding;
import com.redhat.service.bridge.rhoas.dto.AclOperation;
import com.redhat.service.bridge.rhoas.dto.AclPatternType;
import com.redhat.service.bridge.rhoas.dto.AclPermission;
import com.redhat.service.bridge.rhoas.dto.AclResourceType;
import com.redhat.service.bridge.rhoas.dto.Topic;
import com.redhat.service.bridge.rhoas.dto.TopicRequest;
import com.redhat.service.bridge.rhoas.dto.Topics;

import io.smallrye.mutiny.Uni;

@RegisterRestClient
@RegisterClientHeaders(MasSSOHeaderFactory.class)
public interface KafkaInstanceAdminClient {

    @POST
    @Path("/acls")
    Uni<Response> createAcl(AclBinding aclBinding);

    @DELETE
    @Path("/acls")
    Uni<Response> deleteAcl(
            @QueryParam("principal") String principal,
            @QueryParam("permission") AclPermission permission,
            @QueryParam("operation") AclOperation operation,
            @QueryParam("patternType") AclPatternType patternType,
            @QueryParam("patternType") AclResourceType resourceType,
            @QueryParam("resourceName") String resourceName);

    @GET
    @Path("/topics")
    Uni<Topics> listTopics();

    @POST
    @Path("/topics")
    Uni<Topic> createTopic(TopicRequest topic);

    @GET
    @Path("/topics/{name}")
    Uni<Topic> getTopic(@PathParam("name") String name);

    @DELETE
    @Path("/topics/{name}")
    Uni<Response> deleteTopic(@PathParam("name") String name);

}
