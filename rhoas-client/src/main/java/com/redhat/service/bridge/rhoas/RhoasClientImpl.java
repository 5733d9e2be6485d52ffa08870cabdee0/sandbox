package com.redhat.service.bridge.rhoas;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openshift.cloud.api.kas.auth.models.AclBinding;
import com.openshift.cloud.api.kas.auth.models.AclOperation;
import com.openshift.cloud.api.kas.auth.models.AclPatternType;
import com.openshift.cloud.api.kas.auth.models.AclPermissionType;
import com.openshift.cloud.api.kas.auth.models.AclResourceType;
import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.Topic;
import com.openshift.cloud.api.kas.models.ServiceAccount;
import com.openshift.cloud.api.kas.models.ServiceAccountRequest;
import com.redhat.service.bridge.rhoas.exceptions.RhoasClientException;

import io.smallrye.mutiny.CompositeException;
import io.smallrye.mutiny.Uni;

public class RhoasClientImpl implements RhoasClient {

    private static final Logger LOG = LoggerFactory.getLogger(RhoasClientImpl.class);

    private final KafkasMgmtV1Client mgmtClient;
    private final KafkaInstanceAdminClient instanceClient;

    public RhoasClientImpl(KafkasMgmtV1Client mgmtClient, KafkaInstanceAdminClient instanceClient) {
        this.mgmtClient = mgmtClient;
        this.instanceClient = instanceClient;
    }

    @Override
    public Uni<ServiceAccount> createServiceAccount(ServiceAccountRequest serviceAccountRequest) {
        return mgmtClient.createServiceAccount(serviceAccountRequest)
                .onItem().invoke(sa -> LOG.info("Created service account '{}'", sa.getName()))
                .onFailure().transform(f -> logAndWrapFailure("Error when creating service account " + serviceAccountRequest.getName(), f));
    }

    @Override
    public Uni<Void> deleteServiceAccount(String id) {
        return mgmtClient.deleteServiceAccount(id)
                .onItem().invoke(t -> LOG.info("Deleted service account with id '{}'", id))
                .onFailure().transform(f -> logAndWrapFailure("Error when deleting service account with id " + id, f));
    }

    @Override
    public Uni<Topic> createTopic(NewTopicInput newTopicInput) {
        return instanceClient.createTopic(newTopicInput)
                .onItem().invoke(t -> LOG.info("Created topic '{}'", t.getName()))
                .onFailure().transform(f -> logAndWrapFailure("Error when creating topic " + newTopicInput.getName(), f));
    }

    @Override
    public Uni<Topic> createTopicAndGrantAccess(NewTopicInput newTopicInput, String userId, RhoasTopicAccessType accessType) {
        return grantAccess(newTopicInput.getName(), userId, accessType)
                .onItem().transformToUni(v1 -> createTopic(newTopicInput))
                //                        .onFailure().recoverWithUni(f -> recoverCreateTopicAndGrantAccessFailure(newTopicInput, userId, accessType, f)))
                .onFailure().recoverWithUni(f -> recoverCreateTopicAndGrantAccessFailure(newTopicInput, userId, accessType, f));
    }

    private Uni<Topic> recoverCreateTopicAndGrantAccessFailure(NewTopicInput newTopicInput, String userId, RhoasTopicAccessType accessType, Throwable failure) {
        return revokeAccess(newTopicInput.getName(), userId, accessType)
                .onItem().transform(v -> new Topic())
                .onItem().failWith(() -> failure)
                .onFailure().transform(failure2 -> logAndWrapFailures("Multiple errors when creating topic and granting access", failure, failure2));
    }

    @Override
    public Uni<Void> deleteTopic(String topicName) {
        return instanceClient.deleteTopic(topicName)
                .onItem().invoke(t -> LOG.info("Deleted topic '{}'", topicName))
                .onFailure().transform(f -> logAndWrapFailure("Error when deleting topic " + topicName, f));
    }

    @Override
    public Uni<Void> deleteTopicAndRevokeAccess(String topicName, String userId, RhoasTopicAccessType accessType) {
        return revokeAccess(topicName, userId, accessType)
                .onItem().transformToUni(v -> deleteTopic(topicName))
                //                        .onFailure().recoverWithUni(f -> recoverDeleteTopicAndRevokeAccess(topicName, userId, accessType, f)))
                .onFailure().recoverWithUni(f -> recoverDeleteTopicAndRevokeAccess(topicName, userId, accessType, f));
    }

    private Uni<Void> recoverDeleteTopicAndRevokeAccess(String topicName, String userId, RhoasTopicAccessType accessType, Throwable failure) {
        return grantAccess(topicName, userId, accessType)
                .onItem().failWith(() -> failure)
                .onFailure().transform(failure2 -> logAndWrapFailures("Multiple errors when deleting topic and revoking access", failure, failure2));
    }

    @Override
    public Uni<Void> grantAccess(String topicName, String userId, RhoasTopicAccessType accessType) {
        return createACLs(aclsFor(topicName, userId, accessType))
                .onItem().invoke(t -> LOG.info("Created {} ACLs for user='{}' and topic='{}'", accessType.getText(), userId, topicName))
                .onFailure().transform(f -> logAndWrapFailure(
                        "Error when creating " + accessType.getText() + " ACLs for user='" + userId + "' and topic='" + topicName + "'", f));
    }

    @Override
    public Uni<Void> revokeAccess(String topicName, String userId, RhoasTopicAccessType accessType) {
        return deleteACLs(aclsFor(topicName, userId, accessType))
                .onItem().invoke(t -> LOG.info("Deleted {} ACLs for user='{}' and topic='{}'", accessType.getText(), userId, topicName))
                .onFailure().transform(f -> logAndWrapFailure(
                        "Error when deleting " + accessType.getText() + " ACLs for user='" + userId + "' and topic='" + topicName + "'", f));
    }

    private Uni<Void> createACLs(List<AclBinding> acls) {
        return processACLs(acls, instanceClient::createAcl);
    }

    private Uni<Void> deleteACLs(List<AclBinding> acls) {
        return processACLs(acls, instanceClient::deleteAcl);
    }

    private Uni<Void> processACLs(List<AclBinding> acls, Function<AclBinding, Uni<Void>> processor) {
        return Uni.join()
                .all(acls.stream().map(processor).collect(Collectors.toList()))
                .andCollectFailures()
                .replaceWithVoid();
    }

    private List<AclBinding> aclsFor(String topicName, String userId, RhoasTopicAccessType accessType) {
        switch (accessType) {
            case CONSUMER:
                return consumerAclsFor(topicName, userId);
            case PRODUCER:
                return producerAclsFor(topicName, userId);
            case CONSUMER_AND_PRODUCER:
                return consumerAndProducerAclsFor(topicName, userId);
        }
        throw new IllegalStateException("Can't create ACLs for " + accessType);
    }

    private List<AclBinding> consumerAclsFor(String topicName, String userId) {
        return List.of(
                newDescribeTopicAcl(userId, topicName),
                newReadTopicAcl(userId, topicName),
                newReadAllGroupsAcl(userId));
    }

    private List<AclBinding> producerAclsFor(String topicName, String userId) {
        return List.of(
                newDescribeTopicAcl(userId, topicName),
                newWriteTopicAcl(userId, topicName),
                newCreateTopicAcl(userId, topicName),
                newWriteAllTransactionalIdsAcl(userId),
                newDescribeAllTransactionalIdsAcl(userId));
    }

    private List<AclBinding> consumerAndProducerAclsFor(String topicName, String userId) {
        List<AclBinding> permissions = new ArrayList<>();
        permissions.addAll(consumerAclsFor(topicName, userId));
        permissions.addAll(producerAclsFor(topicName, userId));
        return permissions;
    }

    //    @Override
    //    public Uni<TopicAndServiceAccountResponse> createTopicAndConsumerServiceAccount(TopicAndServiceAccountRequest request) {
    //        String topicName = request.getTopicName();
    //        String serviceAccountName = request.getServiceAccountName();
    //        LOG.info("Started creation of topic='{}' and serviceAccount='{}'...", topicName, serviceAccountName);
    //        return Uni.createFrom().item(() -> Context.build(topicName, serviceAccountName))
    //                .onItem().transformToUni(this::createTopic)
    //                .onItem().transformToUni(this::createServiceAccount)
    //                .onItem().transformToUni(this::createServiceAccountACLs)
    //                .onItem().transform(RhoasClientImpl::toResponse);
    //    }
    //
    //    private Uni<Context> createTopic(final Context ctx) {
    //        return instanceClient.createTopic(new NewTopicInput().name(ctx.getTopicName()).settings(new TopicSettings().numPartitions(1)))
    //                .onItem().invoke(t -> LOG.info("Created topic '{}'", t.getName()))
    //                .onItem().transform(ctx::withTopic)
    //                .onFailure().recoverWithUni(failure -> {
    //                    String reason = "Error when creating topic " + ctx.getTopicName();
    //                    LOG.error(reason, failure);
    //                    return Uni.createFrom().item(ctx.withFailure(failure, reason));
    //                });
    //    }
    //
    //    private Uni<Context> revertTopicCreation(final Context ctx) {
    //        LOG.info("Deleting topic '{}' to restore original state after failure", ctx.getTopicName());
    //        return instanceClient.deleteTopic(ctx.getTopicName())
    //                .onItem().transform(v -> {
    //                    LOG.info("Deleted topic '{}'", ctx.getTopicName());
    //                    return ctx;
    //                })
    //                .onFailure().recoverWithUni(failure -> {
    //                    LOG.error("Error when deleting topic " + ctx.getTopicName(), failure);
    //                    return Uni.createFrom().item(ctx.withFailure(failure));
    //                });
    //    }
    //
    //    private Uni<Context> createServiceAccount(final Context ctx) {
    //        if (ctx.hasFailures()) {
    //            return Uni.createFrom().item(ctx);
    //        }
    //        return mgmtClient.createServiceAccount(new ServiceAccountRequest().name(ctx.getServiceAccountName()))
    //                .onItem().invoke(sa -> LOG.info("Created service account id='{}', clientId='{}', clientSecret='{}'", sa.getId(), sa.getClientId(), sa.getClientSecret()))
    //                .onItem().transform(ctx::withServiceAccount)
    //                .onFailure().recoverWithUni(failure -> {
    //                    String reason = "Error when creating service account " + ctx.getServiceAccountName();
    //                    LOG.error(reason, failure);
    //                    return Uni.createFrom().item(ctx.withFailure(failure, reason))
    //                            .onItem().transformToUni(this::revertTopicCreation);
    //                });
    //    }
    //
    //    private Uni<Context> revertServiceAccountCreation(final Context ctx) {
    //        final String serviceAccountId = ctx.getServiceAccount().getId();
    //        LOG.info("Deleting service account '{}' to restore original state after failure", serviceAccountId);
    //        return mgmtClient.deleteServiceAccount(serviceAccountId)
    //                .onItem().transform(v -> {
    //                    LOG.info("Deleted service account '{}'", serviceAccountId);
    //                    return ctx;
    //                })
    //                .onFailure().recoverWithUni(failure -> {
    //                    LOG.error("Error when deleting service account " + serviceAccountId, failure);
    //                    return Uni.createFrom().item(ctx.withFailure(failure));
    //                });
    //    }
    //
    //    private Uni<Context> createServiceAccountACLs(final Context ctx) {
    //        if (ctx.hasFailures()) {
    //            return Uni.createFrom().item(ctx);
    //        }
    //
    //        String clientId = ctx.getServiceAccount().getClientId();
    //        String topicName = ctx.getTopicName();
    //
    //        final List<AclBinding> acls = List.of(
    //                newDescribeTopicAcl(clientId, topicName),
    //                newReadTopicAcl(clientId, topicName),
    //                newReadAllGroupsAcl(clientId));
    //
    //        return Uni.join().all(acls.stream().map(instanceClient::createAcl).collect(Collectors.toList()))
    //                .andCollectFailures()
    //                .onItem().invoke(sa -> LOG.info("Created ACLs"))
    //                .onItem().transform(l -> ctx)
    //                .onFailure().recoverWithUni(failure -> {
    //                    String reason = "Error when creating service account ACLs for " + ctx.getServiceAccountName();
    //                    LOG.error(reason, failure);
    //                    return Uni.createFrom().item(ctx.withFailure(failure, reason))
    //                            .onItem().transformToUni(this::revertTopicCreation)
    //                            .onItem().transformToUni(this::revertServiceAccountCreation)
    //                            .onItem().transformToUni(this::revertServiceAccountACLsCreation);
    //                });
    //    }
    //
    //    private Uni<Context> revertServiceAccountACLsCreation(final Context ctx) {
    //        String clientId = ctx.getServiceAccount().getClientId();
    //        String topicName = ctx.getTopicName();
    //
    //        final List<AclBinding> acls = List.of(
    //                newDescribeTopicAcl(clientId, topicName),
    //                newReadTopicAcl(clientId, topicName),
    //                newReadAllGroupsAcl(clientId));
    //
    //        LOG.info("Deleting ACLs to restore original state after failure");
    //        return Uni.join().all(acls.stream().map(instanceClient::deleteAcl).collect(Collectors.toList()))
    //                .andCollectFailures()
    //                .onItem().transform(v -> {
    //                    LOG.info("Deleted ACLs");
    //                    return ctx;
    //                })
    //                .onFailure().recoverWithUni(failure -> {
    //                    LOG.error("Error when deleting ACLs", failure);
    //                    return Uni.createFrom().item(ctx.withFailure(failure));
    //                });
    //    }

    private static AclBinding newDescribeTopicAcl(String userId, String topicName) {
        return new AclBinding()
                .resourceType(AclResourceType.TOPIC)
                .resourceName(topicName)
                .patternType(AclPatternType.LITERAL)
                .principal(toPrincipal(userId))
                .operation(AclOperation.DESCRIBE)
                .permission(AclPermissionType.ALLOW);
    }

    private static AclBinding newCreateTopicAcl(String userId, String topicName) {
        return new AclBinding()
                .resourceType(AclResourceType.TOPIC)
                .resourceName(topicName)
                .patternType(AclPatternType.LITERAL)
                .principal(toPrincipal(userId))
                .operation(AclOperation.CREATE)
                .permission(AclPermissionType.ALLOW);
    }

    private static AclBinding newReadTopicAcl(String userId, String topicName) {
        return new AclBinding()
                .resourceType(AclResourceType.TOPIC)
                .resourceName(topicName)
                .patternType(AclPatternType.LITERAL)
                .principal(toPrincipal(userId))
                .operation(AclOperation.READ)
                .permission(AclPermissionType.ALLOW);
    }

    private static AclBinding newWriteTopicAcl(String userId, String topicName) {
        return new AclBinding()
                .resourceType(AclResourceType.TOPIC)
                .resourceName(topicName)
                .patternType(AclPatternType.LITERAL)
                .principal(toPrincipal(userId))
                .operation(AclOperation.WRITE)
                .permission(AclPermissionType.ALLOW);
    }

    private static AclBinding newReadAllGroupsAcl(String userId) {
        return new AclBinding()
                .resourceType(AclResourceType.GROUP)
                .resourceName("*")
                .patternType(AclPatternType.LITERAL)
                .principal(toPrincipal(userId))
                .operation(AclOperation.READ)
                .permission(AclPermissionType.ALLOW);
    }

    private static AclBinding newDescribeAllTransactionalIdsAcl(String userId) {
        return new AclBinding()
                .resourceType(AclResourceType.TRANSACTIONAL_ID)
                .resourceName("*")
                .patternType(AclPatternType.LITERAL)
                .principal(toPrincipal(userId))
                .operation(AclOperation.DESCRIBE)
                .permission(AclPermissionType.ALLOW);
    }

    private static AclBinding newWriteAllTransactionalIdsAcl(String userId) {
        return new AclBinding()
                .resourceType(AclResourceType.TRANSACTIONAL_ID)
                .resourceName("*")
                .patternType(AclPatternType.LITERAL)
                .principal(toPrincipal(userId))
                .operation(AclOperation.WRITE)
                .permission(AclPermissionType.ALLOW);
    }

    private static String toPrincipal(String userId) {
        return "User:" + userId;
    }

    //    private static TopicAndServiceAccountResponse toResponse(Context ctx) {
    //        if (ctx.hasFailures()) {
    //            throw new RhoasClientException(ctx.getReason(), ctx.getFailures());
    //        }
    //        LOG.info("Creation completed of topic='{}' and serviceAccount='{}'", ctx.getTopicName(), ctx.getServiceAccountName());
    //        return new TopicAndServiceAccountResponse(ctx.getTopic(), ctx.getServiceAccount());
    //    }

    private static RhoasClientException logAndWrapFailure(String reason, Throwable failure) {
        LOG.error(reason, failure);
        return new RhoasClientException(reason, failure);
    }

    private static RhoasClientException logAndWrapFailures(String reason, Throwable failure1, Throwable failure2) {
        CompositeException compositeFailure = new CompositeException(failure1, failure2);
        return logAndWrapFailure(reason, compositeFailure);
    }

    //    private static class Context {
    //        private final String topicName;
    //        private final String serviceAccountName;
    //        private final Topic topic;
    //        private final ServiceAccount serviceAccount;
    //        private final List<Throwable> failures;
    //        private final String reason;
    //
    //        public Context(String topicName, String serviceAccountName, Topic topic, ServiceAccount serviceAccount, List<Throwable> failures,
    //                String reason) {
    //            this.topicName = topicName;
    //            this.serviceAccountName = serviceAccountName;
    //            this.topic = topic;
    //            this.serviceAccount = serviceAccount;
    //            this.failures = failures;
    //            this.reason = reason;
    //        }
    //
    //        public String getTopicName() {
    //            return topicName;
    //        }
    //
    //        public String getServiceAccountName() {
    //            return serviceAccountName;
    //        }
    //
    //        public Topic getTopic() {
    //            return topic;
    //        }
    //
    //        public ServiceAccount getServiceAccount() {
    //            return serviceAccount;
    //        }
    //
    //        public List<Throwable> getFailures() {
    //            return failures;
    //        }
    //
    //        public String getReason() {
    //            return reason;
    //        }
    //
    //        public boolean hasFailures() {
    //            return failures != null && !failures.isEmpty();
    //        }
    //
    //        public Context withTopic(Topic newTopic) {
    //            return new Context(topicName, serviceAccountName, newTopic, serviceAccount, failures, reason);
    //        }
    //
    //        public Context withServiceAccount(ServiceAccount newServiceAccount) {
    //            return new Context(topicName, serviceAccountName, topic, newServiceAccount, failures, reason);
    //        }
    //
    //        public Context withFailure(Throwable failure) {
    //            return withFailure(failure, null);
    //        }
    //
    //        public Context withFailure(Throwable failure, String reason) {
    //            List<Throwable> newFailures = new ArrayList<>(failures != null ? failures.size() + 1 : 1);
    //            if (failures != null) {
    //                newFailures.addAll(failures);
    //            }
    //            newFailures.add(failure);
    //            String newReason = this.reason == null ? reason : this.reason;
    //            return new Context(topicName, serviceAccountName, topic, serviceAccount, Collections.unmodifiableList(newFailures), newReason);
    //        }
    //
    //        public static Context build(String topicName, String serviceAccountName) {
    //            return new Context(topicName, serviceAccountName, null, null, null, null);
    //        }
    //    }
}
