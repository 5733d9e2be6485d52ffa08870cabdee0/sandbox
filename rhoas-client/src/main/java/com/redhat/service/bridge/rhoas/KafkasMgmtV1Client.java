package com.redhat.service.bridge.rhoas;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.redhat.service.bridge.rhoas.auth.RedHatSSOHeaderFactory;
import com.redhat.service.bridge.rhoas.dto.ServiceAccount;
import com.redhat.service.bridge.rhoas.dto.ServiceAccountRequest;
import com.redhat.service.bridge.rhoas.dto.ServiceAccounts;

import io.smallrye.mutiny.Uni;

@RegisterRestClient
@RegisterClientHeaders(RedHatSSOHeaderFactory.class)
public interface KafkasMgmtV1Client {

    @GET
    @Path("/service_accounts")
    Uni<ServiceAccounts> listServiceAccounts();

    @POST
    @Path("/service_accounts")
    Uni<ServiceAccount> createServiceAccount(ServiceAccountRequest serviceAccount);

    @GET
    @Path("/service_accounts/{id}")
    Uni<ServiceAccount> getServiceAccount(@PathParam("id") String id);

    @DELETE
    @Path("/service_accounts/{id}")
    Uni<Response> deleteServiceAccount(@PathParam("id") String id);

}
