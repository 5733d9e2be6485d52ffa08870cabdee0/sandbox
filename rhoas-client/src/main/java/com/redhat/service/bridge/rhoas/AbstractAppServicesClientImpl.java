package com.redhat.service.bridge.rhoas;

import java.util.function.Supplier;

import com.openshift.cloud.api.kas.SecurityApi;
import com.openshift.cloud.api.kas.auth.AclsApi;
import com.openshift.cloud.api.kas.auth.TopicsApi;
import com.redhat.service.bridge.rhoas.exceptions.AppServicesException;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

abstract class AbstractAppServicesClientImpl {

    private Vertx vertx;

    protected abstract String getAccessToken();

    protected abstract String getBasePath();

    protected void init(Vertx vertx) {
        this.vertx = vertx;
    }

    protected <T> Uni<T> aclsValueCall(ApiValueExecutor<AclsApi, T> executor) {
        return executeBlocking(() -> executeAndHandleException(new AclsApi(defaultInstanceClient()), executor));
    }

    protected Uni<Void> aclsVoidCall(ApiVoidExecutor<AclsApi> executor) {
        return executeBlocking(() -> executeAndHandleException(new AclsApi(defaultInstanceClient()), executor)).replaceWithVoid();
    }

    protected <T> Uni<T> topicsValueCall(ApiValueExecutor<TopicsApi, T> executor) {
        return executeBlocking(() -> executeAndHandleException(new TopicsApi(defaultInstanceClient()), executor));
    }

    protected Uni<Void> topicsVoidCall(ApiVoidExecutor<TopicsApi> executor) {
        return executeBlocking(() -> executeAndHandleException(new TopicsApi(defaultInstanceClient()), executor)).replaceWithVoid();
    }

    protected <T> Uni<T> securityValueCall(ApiValueExecutor<SecurityApi, T> executor) {
        return executeBlocking(() -> executeAndHandleException(new SecurityApi(defaultMgmtClient()), executor));
    }

    protected Uni<Void> securityVoidCall(ApiVoidExecutor<SecurityApi> executor) {
        return executeBlocking(() -> executeAndHandleException(new SecurityApi(defaultMgmtClient()), executor)).replaceWithVoid();
    }

    private com.openshift.cloud.api.kas.invoker.ApiClient defaultMgmtClient() {
        com.openshift.cloud.api.kas.invoker.ApiClient defaultClient = com.openshift.cloud.api.kas.invoker.Configuration.getDefaultApiClient();
        defaultClient.setBasePath(getBasePath());

        // Configure HTTP bearer authorization: Bearer
        com.openshift.cloud.api.kas.invoker.auth.HttpBearerAuth bearer = (com.openshift.cloud.api.kas.invoker.auth.HttpBearerAuth) defaultClient.getAuthentication("Bearer");
        bearer.setBearerToken(getAccessToken());

        return defaultClient;
    }

    private com.openshift.cloud.api.kas.auth.invoker.ApiClient defaultInstanceClient() {
        com.openshift.cloud.api.kas.auth.invoker.ApiClient defaultClient = com.openshift.cloud.api.kas.auth.invoker.Configuration.getDefaultApiClient();
        defaultClient.setBasePath(getBasePath());

        // Configure OAuth2 access token for authorization: Bearer
        com.openshift.cloud.api.kas.auth.invoker.auth.OAuth bearer = (com.openshift.cloud.api.kas.auth.invoker.auth.OAuth) defaultClient.getAuthentication("Bearer");
        bearer.setAccessToken(getAccessToken());

        return defaultClient;
    }

    private <T> Uni<T> executeBlocking(Supplier<T> supplier) {
        return vertx.executeBlocking(Uni.createFrom().item(supplier));
    }

    private <A, T> T executeAndHandleException(A apiInstance, ApiValueExecutor<A, T> executor) {
        try {
            return executor.execute(apiInstance);
        } catch (com.openshift.cloud.api.kas.invoker.ApiException e) {
            throw new AppServicesException("Error when calling Kafka mgmt " + apiInstance.getClass().getSimpleName(), e);
        } catch (com.openshift.cloud.api.kas.auth.invoker.ApiException e) {
            throw new AppServicesException("Error when calling Kafka instance " + apiInstance.getClass().getSimpleName(), e);
        }
    }

    private <A> Object executeAndHandleException(A apiInstance, ApiVoidExecutor<A> executor) {
        return executeAndHandleException(apiInstance, api -> {
            executor.execute(apiInstance);
            return null;
        });
    }

    protected interface ApiValueExecutor<A, T> {
        T execute(A api) throws com.openshift.cloud.api.kas.invoker.ApiException, com.openshift.cloud.api.kas.auth.invoker.ApiException;
    }

    protected interface ApiVoidExecutor<A> {
        void execute(A api) throws com.openshift.cloud.api.kas.invoker.ApiException, com.openshift.cloud.api.kas.auth.invoker.ApiException;
    }

}
