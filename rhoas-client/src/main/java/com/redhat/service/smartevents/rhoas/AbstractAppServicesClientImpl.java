package com.redhat.service.smartevents.rhoas;

import java.util.function.Supplier;

import com.openshift.cloud.api.kas.SecurityApi;
import com.openshift.cloud.api.kas.auth.AclsApi;
import com.openshift.cloud.api.kas.auth.TopicsApi;
import com.redhat.service.smartevents.rhoas.exceptions.AppServicesException;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

/**
 * Base class that contains the common logic to configure kafka-management-sdk and kafka-instance-sdk clients
 * and to execute their synchronous calls in a reactive async way (sdks are only sync at the moment).
 * It allows its subclasses ({@link KafkasMgmtV1ClientImpl} and {@link KafkaInstanceAdminClientImpl} to implement
 * the specific calls with the minimum amout of duplicated code.
 */
abstract class AbstractAppServicesClientImpl {

    protected final Vertx vertx;

    protected AbstractAppServicesClientImpl(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * Must be reimplemented by the subclass to return the access token
     *
     * @return the access token
     */
    protected abstract String getAccessToken();

    /**
     * Must be reimplemented by the subclass to return the base path
     *
     * @return the base path
     */
    protected abstract String getBasePath();

    /**
     * Execute a specific call to {@link AclsApi} returning a value in a reactive async way
     *
     * @param executor the executor wrapping the call to be invoked
     * @param <T> the return type
     * @return {@link Uni} wrapping the execution and containing the value returned by the api
     */
    protected <T> Uni<T> aclsValueCall(ApiValueExecutor<AclsApi, T> executor) {
        return executeBlocking(() -> executeAndHandleException(new AclsApi(defaultInstanceClient()), executor));
    }

    /**
     * Execute a specific call to {@link AclsApi} returning void in a reactive async way
     *
     * @param executor the executor wrapping the call to be invoked
     * @return {@link Uni<Void>} wrapping the execution
     */
    protected Uni<Void> aclsVoidCall(ApiVoidExecutor<AclsApi> executor) {
        return executeBlocking(() -> executeAndHandleException(new AclsApi(defaultInstanceClient()), executor)).replaceWithVoid();
    }

    /**
     * Execute a specific call to {@link TopicsApi} returning a value in a reactive async way
     *
     * @param executor the executor wrapping the call to be invoked
     * @param <T> the return type
     * @return {@link Uni} wrapping the execution and containing the value returned by the api
     */
    protected <T> Uni<T> topicsValueCall(ApiValueExecutor<TopicsApi, T> executor) {
        return executeBlocking(() -> executeAndHandleException(new TopicsApi(defaultInstanceClient()), executor));
    }

    /**
     * Execute a specific call to {@link TopicsApi} returning void in a reactive async way
     *
     * @param executor the executor wrapping the call to be invoked
     * @return {@link Uni<Void>} wrapping the execution
     */
    protected Uni<Void> topicsVoidCall(ApiVoidExecutor<TopicsApi> executor) {
        return executeBlocking(() -> executeAndHandleException(new TopicsApi(defaultInstanceClient()), executor)).replaceWithVoid();
    }

    /**
     * Execute a specific call to {@link SecurityApi} returning a value in a reactive async way
     *
     * @param executor the executor wrapping the call to be invoked
     * @param <T> the return type
     * @return {@link Uni} wrapping the execution and containing the value returned by the api
     */
    protected <T> Uni<T> securityValueCall(ApiValueExecutor<SecurityApi, T> executor) {
        return executeBlocking(() -> executeAndHandleException(new SecurityApi(defaultMgmtClient()), executor));
    }

    /**
     * Execute a specific call to {@link SecurityApi} returning void in a reactive async way
     *
     * @param executor the executor wrapping the call to be invoked
     * @return {@link Uni<Void>} wrapping the execution
     */
    protected Uni<Void> securityVoidCall(ApiVoidExecutor<SecurityApi> executor) {
        return executeBlocking(() -> executeAndHandleException(new SecurityApi(defaultMgmtClient()), executor)).replaceWithVoid();
    }

    /**
     * Creates a kafka-management-sdk client configured with the correct base path and auth token
     *
     * @return the configured client
     */
    private com.openshift.cloud.api.kas.invoker.ApiClient defaultMgmtClient() {
        com.openshift.cloud.api.kas.invoker.ApiClient defaultClient = com.openshift.cloud.api.kas.invoker.Configuration.getDefaultApiClient();
        defaultClient.setBasePath(getBasePath());

        // Configure HTTP bearer authorization: Bearer
        com.openshift.cloud.api.kas.invoker.auth.HttpBearerAuth bearer = (com.openshift.cloud.api.kas.invoker.auth.HttpBearerAuth) defaultClient.getAuthentication("Bearer");
        bearer.setBearerToken(getAccessToken());

        return defaultClient;
    }

    /**
     * Creates a kafka-instance-sdk client configured with the correct base path and auth token
     *
     * @return the configured client
     */
    private com.openshift.cloud.api.kas.auth.invoker.ApiClient defaultInstanceClient() {
        com.openshift.cloud.api.kas.auth.invoker.ApiClient defaultClient = com.openshift.cloud.api.kas.auth.invoker.Configuration.getDefaultApiClient();
        defaultClient.setBasePath(getBasePath());

        // Configure OAuth2 access token for authorization: Bearer
        com.openshift.cloud.api.kas.auth.invoker.auth.OAuth bearer = (com.openshift.cloud.api.kas.auth.invoker.auth.OAuth) defaultClient.getAuthentication("Bearer");
        bearer.setAccessToken(getAccessToken());

        return defaultClient;
    }

    /**
     * Execute blocking code in reactive async way using {@link Vertx#executeBlocking(Uni)}
     *
     * @param supplier the lambda containing the blocking code
     * @param <T> the return type
     * @return {@link Uni} wrapping the blocking code
     */
    private <T> Uni<T> executeBlocking(Supplier<T> supplier) {
        return vertx.executeBlocking(Uni.createFrom().item(supplier));
    }

    /**
     * Execute the desired (call returning a value) on the desired API client, handling possible exceptions.
     * This is a pure synchronous execution.
     *
     * @param apiInstance the API client to be used
     * @param executor the executor wrapping the call to be invoked
     * @param <A> the API client class type
     * @param <T> the return type
     * @return the value returned by the call
     */
    private <A, T> T executeAndHandleException(A apiInstance, ApiValueExecutor<A, T> executor) {
        try {
            return executor.execute(apiInstance);
        } catch (com.openshift.cloud.api.kas.invoker.ApiException e) {
            throw new AppServicesException("Error when calling Kafka mgmt " + apiInstance.getClass().getSimpleName(), e);
        } catch (com.openshift.cloud.api.kas.auth.invoker.ApiException e) {
            throw new AppServicesException("Error when calling Kafka instance " + apiInstance.getClass().getSimpleName(), e);
        }
    }

    /**
     * Execute the desired (call returning void) on the desired API client, handling possible exceptions.
     * This is a pure synchronous execution.
     *
     * @param apiInstance the API client to be used
     * @param executor the executor wrapping the call to be invoked
     * @param <A> the API client class type
     * @return a null {@link Object} value that will be wrapped to {@link Uni<Void>} later
     */
    private <A> Object executeAndHandleException(A apiInstance, ApiVoidExecutor<A> executor) {
        return executeAndHandleException(apiInstance, api -> {
            executor.execute(apiInstance);
            return null;
        });
    }

    /**
     * This interface allows to define a lambda that receives a configured API class and returns a specific value.
     * It handles the specific checked exceptions.
     * It is supposed to be used to wrap api calls that returns a desired value.
     *
     * @param <A> The API class type (e.g. {@link AclsApi}, {@link TopicsApi} and {@link SecurityApi})
     * @param <T> The return type
     */
    protected interface ApiValueExecutor<A, T> {
        T execute(A api) throws com.openshift.cloud.api.kas.invoker.ApiException, com.openshift.cloud.api.kas.auth.invoker.ApiException;
    }

    /**
     * This interface allows to define a lambda that receives a configured API class and returns void.
     * It handles the specific checked exceptions.
     * It is supposed to be used to wrap api calls that either don't return a value or whose return value can be ignored.
     *
     * @param <A> The API class type (e.g. {@link AclsApi}, {@link TopicsApi} and {@link SecurityApi})
     */
    protected interface ApiVoidExecutor<A> {
        void execute(A api) throws com.openshift.cloud.api.kas.invoker.ApiException, com.openshift.cloud.api.kas.auth.invoker.ApiException;
    }

}

