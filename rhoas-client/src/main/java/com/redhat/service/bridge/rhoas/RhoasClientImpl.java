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
                .onFailure().recoverWithUni(f -> recoverDeleteTopicAndRevokeAccess(topicName, userId, accessType, f));
    }

    private Uni<Void> recoverDeleteTopicAndRevokeAccess(String topicName, String userId, RhoasTopicAccessType accessType, Throwable failure) {
        return grantAccess(topicName, userId, accessType)
                .onItem().failWith(() -> failure)
                .onFailure().transform(failure2 -> logAndWrapFailures("Multiple errors when deleting topic and revoking access", failure, failure2));
    }

    @Override
    public Uni<Void> grantAccess(String topicName, String userId, RhoasTopicAccessType accessType) {
        return createACLs(grantAclsFor(topicName, userId, accessType))
                .onItem().invoke(t -> LOG.info("Created {} ACLs for user='{}' and topic='{}'", accessType.getText(), userId, topicName))
                .onFailure().transform(f -> logAndWrapFailure(
                        "Error when creating " + accessType.getText() + " ACLs for user='" + userId + "' and topic='" + topicName + "'", f));
    }

    @Override
    public Uni<Void> revokeAccess(String topicName, String userId, RhoasTopicAccessType accessType) {
        return deleteACLs(revokeAclsFor(topicName, userId, accessType))
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

    private List<AclBinding> grantAclsFor(String topicName, String userId, RhoasTopicAccessType accessType) {
        switch (accessType) {
            case CONSUMER:
                return grantConsumerAclsFor(topicName, userId);
            case PRODUCER:
                return grantProducerAclsFor(topicName, userId);
            case CONSUMER_AND_PRODUCER:
                return joinAcls(grantConsumerAclsFor(topicName, userId), grantProducerAclsFor(topicName, userId));
        }
        throw new IllegalStateException("Can't create ACLs for " + accessType);
    }

    private List<AclBinding> grantConsumerAclsFor(String topicName, String userId) {
        return List.of(
                newDescribeTopicAcl(topicName, userId),
                newReadTopicAcl(topicName, userId),
                newReadAllGroupsAcl(userId));
    }

    private List<AclBinding> grantProducerAclsFor(String topicName, String userId) {
        return List.of(
                newDescribeTopicAcl(topicName, userId),
                newWriteTopicAcl(topicName, userId),
                newCreateTopicAcl(topicName, userId),
                newWriteAllTransactionalIdsAcl(userId),
                newDescribeAllTransactionalIdsAcl(userId));
    }

    private List<AclBinding> revokeAclsFor(String topicName, String userId, RhoasTopicAccessType accessType) {
        switch (accessType) {
            case CONSUMER:
                return revokeConsumerAclsFor(topicName, userId);
            case PRODUCER:
                return revokeProducerAclsFor(topicName, userId);
            case CONSUMER_AND_PRODUCER:
                return joinAcls(revokeConsumerAclsFor(topicName, userId), revokeProducerAclsFor(topicName, userId));
        }
        throw new IllegalStateException("Can't create ACLs for " + accessType);
    }

    private List<AclBinding> revokeConsumerAclsFor(String topicName, String userId) {
        return List.of(
                newDescribeTopicAcl(topicName, userId),
                newReadTopicAcl(topicName, userId));
    }

    private List<AclBinding> revokeProducerAclsFor(String topicName, String userId) {
        return List.of(
                newDescribeTopicAcl(topicName, userId),
                newWriteTopicAcl(topicName, userId),
                newCreateTopicAcl(topicName, userId));
    }

    private List<AclBinding> joinAcls(List<AclBinding> acls1, List<AclBinding> acls2) {
        List<AclBinding> permissions = new ArrayList<>();
        permissions.addAll(acls1);
        permissions.addAll(acls2);
        return permissions;
    }

    private static AclBinding newDescribeTopicAcl(String topicName, String userId) {
        return new AclBinding()
                .resourceType(AclResourceType.TOPIC)
                .resourceName(topicName)
                .patternType(AclPatternType.LITERAL)
                .principal(toPrincipal(userId))
                .operation(AclOperation.DESCRIBE)
                .permission(AclPermissionType.ALLOW);
    }

    private static AclBinding newCreateTopicAcl(String topicName, String userId) {
        return new AclBinding()
                .resourceType(AclResourceType.TOPIC)
                .resourceName(topicName)
                .patternType(AclPatternType.LITERAL)
                .principal(toPrincipal(userId))
                .operation(AclOperation.CREATE)
                .permission(AclPermissionType.ALLOW);
    }

    private static AclBinding newReadTopicAcl(String topicName, String userId) {
        return new AclBinding()
                .resourceType(AclResourceType.TOPIC)
                .resourceName(topicName)
                .patternType(AclPatternType.LITERAL)
                .principal(toPrincipal(userId))
                .operation(AclOperation.READ)
                .permission(AclPermissionType.ALLOW);
    }

    private static AclBinding newWriteTopicAcl(String topicName, String userId) {
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

    private static RhoasClientException logAndWrapFailure(String reason, Throwable failure) {
        LOG.error(reason, failure);
        return new RhoasClientException(reason, failure);
    }

    private static RhoasClientException logAndWrapFailures(String reason, Throwable failure1, Throwable failure2) {
        CompositeException compositeFailure = new CompositeException(failure1, failure2);
        return logAndWrapFailure(reason, compositeFailure);
    }
}
