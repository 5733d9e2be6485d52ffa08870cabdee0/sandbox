package com.redhat.service.bridge.external.ansiblegateway;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

@Path("/api/v2")
@Consumes("application/json")
@Produces("application/json")
@RegisterRestClient(configKey="ansible-tower-v2")
@RegisterClientHeaders(AnsibleTowerHeaderFactory.class)
public interface AnsibleTowerClient {

    @POST
    @Path("/job_templates/{jobTemplateId}/launch/")
    Response launchJobTemplate(@PathParam Integer jobTemplateId);

}
