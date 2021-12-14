package com.redhat.service.bridge.rhoas;

import java.util.concurrent.CompletionStage;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.redhat.service.bridge.rhoas.auth.RedHatSSOHeaderFactory;
import com.redhat.service.bridge.rhoas.dto.ServiceAccount;
import com.redhat.service.bridge.rhoas.dto.ServiceAccounts;
import com.redhat.service.bridge.rhoas.request.ServiceAccountRequest;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient
@RegisterClientHeaders(RedHatSSOHeaderFactory.class)
public interface KafkasMgmtV1Client {

    @GET
    @Path("/service_accounts")
    CompletionStage<ServiceAccounts> listServiceAccounts();

    @POST
    @Path("/service_accounts")
    CompletionStage<ServiceAccount> createServiceAccount(ServiceAccountRequest request);

    @GET
    @Path("/service_accounts/{id}")
    CompletionStage<ServiceAccount> getServiceAccount(@PathParam("id") String id);

    @DELETE
    @Path("/service_accounts/{id}")
    CompletionStage<Void> deleteServiceAccount(@PathParam("id") String id);

}
