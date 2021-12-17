package com.redhat.service.bridge.rhoas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.rhoas.dto.AclBinding;
import com.redhat.service.bridge.rhoas.dto.AclOperation;
import com.redhat.service.bridge.rhoas.dto.AclPatternType;
import com.redhat.service.bridge.rhoas.dto.AclPermission;
import com.redhat.service.bridge.rhoas.dto.AclResourceType;
import com.redhat.service.bridge.rhoas.dto.ServiceAccount;
import com.redhat.service.bridge.rhoas.dto.ServiceAccountRequest;
import com.redhat.service.bridge.rhoas.dto.Topic;
import com.redhat.service.bridge.rhoas.dto.TopicAndServiceAccount;
import com.redhat.service.bridge.rhoas.dto.TopicRequest;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class RhoasServiceImpl implements RhoasService {

    private static final Logger LOG = LoggerFactory.getLogger(RhoasServiceImpl.class);

    @Inject
    @RestClient
    KafkasMgmtV1Client mgmtClient;

    @Inject
    @RestClient
    KafkaInstanceAdminClient instanceClient;

    @Override
    public Uni<TopicAndServiceAccount> createTopicAndConsumerServiceAccount(final String topicName, final String serviceAccountName) {
        LOG.info("Started creation of topic={} and serviceAccount={}...", topicName, serviceAccountName);
        return Uni.createFrom().item(() -> Context.build(topicName, serviceAccountName))
                .onItem().transformToUni(this::createTopic)
                .onItem().transformToUni(this::createServiceAccount)
                .onItem().transformToUni(this::createServiceAccountACLs)
                .onItem().transform(ctx -> {
                    if (ctx.hasFailures()) {
                        throw new RhoasServiceException(ctx.getReason(), ctx.getFailures());
                    }
                    LOG.info("Creation completed of topic={} and serviceAccount={}", ctx.getTopicName(), ctx.getServiceAccountName());
                    return new TopicAndServiceAccount(ctx.getTopic(), ctx.getServiceAccount());
                });
    }

    private Uni<Context> createTopic(final Context ctx) {
        return instanceClient.createTopic(new TopicRequest(ctx.getTopicName()))
                .onItem().invoke(t -> LOG.info("Created topic {}", t.getName()))
                .onItem().transform(ctx::withTopic)
                .onFailure().recoverWithUni(failure -> {
                    String reason = "Error when creating topic " + ctx.getTopicName();
                    LOG.error(reason, failure);
                    return Uni.createFrom().item(ctx.withFailure(failure, reason));
                });
    }

    private Uni<Context> revertTopicCreation(final Context ctx) {
        LOG.info("Deleting topic {} to restore original state after failure", ctx.getTopicName());
        return instanceClient.deleteTopic(ctx.getTopicName())
                .onItem().transform(v -> {
                    LOG.info("Deleted topic {}", ctx.getTopicName());
                    return ctx;
                })
                .onFailure().recoverWithUni(failure -> {
                    LOG.error("Error when deleting topic " + ctx.getTopicName(), failure);
                    return Uni.createFrom().item(ctx.withFailure(failure));
                });
    }

    private Uni<Context> createServiceAccount(final Context ctx) {
        if (ctx.hasFailures()) {
            return Uni.createFrom().item(ctx);
        }
        return mgmtClient.createServiceAccount(new ServiceAccountRequest(ctx.getServiceAccountName()))
                .onItem().invoke(sa -> LOG.info("Created service account id={}, clientId={}, clientSecret={}", sa.getId(), sa.getClientId(), sa.getClientSecret()))
                .onItem().transform(ctx::withServiceAccount)
                .onFailure().recoverWithUni(failure -> {
                    String reason = "Error when creating service account " + ctx.getServiceAccountName();
                    LOG.error(reason, failure);
                    return Uni.createFrom().item(ctx.withFailure(failure, reason))
                            .onItem().transformToUni(this::revertTopicCreation);
                });
    }

    private Uni<Context> revertServiceAccountCreation(final Context ctx) {
        final String serviceAccountId = ctx.getServiceAccount().getId();
        LOG.info("Deleting service account {} to restore original state after failure", serviceAccountId);
        return mgmtClient.deleteServiceAccount(serviceAccountId)
                .onItem().transform(v -> {
                    LOG.info("Deleted service account {}", serviceAccountId);
                    return ctx;
                })
                .onFailure().recoverWithUni(failure -> {
                    LOG.error("Error when deleting service account " + serviceAccountId, failure);
                    return Uni.createFrom().item(ctx.withFailure(failure));
                });
    }

    private Uni<Context> createServiceAccountACLs(final Context ctx) {
        if (ctx.hasFailures()) {
            return Uni.createFrom().item(ctx);
        }

        String clientId = ctx.getServiceAccount().getClientId();
        String topicName = ctx.getTopicName();

        final List<AclBinding> acls = List.of(
                newDescribeTopicAcl(clientId, topicName),
                newReadTopicAcl(clientId, topicName),
                newReadAllGroupsAcl(clientId));

        return Uni.join().all(acls.stream().map(instanceClient::createAcl).collect(Collectors.toList()))
                .andCollectFailures()
                .onItem().invoke(sa -> LOG.info("Created ACLs"))
                .onItem().transform(l -> ctx)
                .onFailure().recoverWithUni(failure -> {
                    String reason = "Error when creating service account ACLs for " + ctx.getServiceAccountName();
                    LOG.error(reason, failure);
                    return Uni.createFrom().item(ctx.withFailure(failure, reason))
                            .onItem().transformToUni(this::revertTopicCreation)
                            .onItem().transformToUni(this::revertServiceAccountCreation)
                            .onItem().transformToUni(this::revertServiceAccountACLsCreation);
                });
    }

    private Uni<Context> revertServiceAccountACLsCreation(final Context ctx) {
        String clientId = ctx.getServiceAccount().getClientId();
        String topicName = ctx.getTopicName();

        final List<AclBinding> acls = List.of(
                newDescribeTopicAcl(clientId, topicName),
                newReadTopicAcl(clientId, topicName),
                newReadAllGroupsAcl(clientId));

        LOG.info("Deleting ACLs to restore original state after failure");
        return Uni.join().all(acls.stream().map(this::deleteServiceAccountACL).collect(Collectors.toList()))
                .andCollectFailures()
                .onItem().transform(v -> {
                    LOG.info("Deleted ACLs");
                    return ctx;
                })
                .onFailure().recoverWithUni(failure -> {
                    LOG.error("Error when deleting ACLs", failure);
                    return Uni.createFrom().item(ctx.withFailure(failure));
                });
    }

    private Uni<Response> deleteServiceAccountACL(AclBinding acl) {
        return instanceClient.deleteAcl(
                acl.getPrincipal(),
                acl.getPermission(),
                acl.getOperation(),
                acl.getPatternType(),
                acl.getResourceType(),
                acl.getResourceName());
    }

    private static AclBinding newDescribeTopicAcl(String clientId, String topicName) {
        return new AclBinding(
                clientId,
                AclPermission.ALLOW,
                AclOperation.DESCRIBE,
                AclPatternType.LITERAL,
                AclResourceType.TOPIC,
                topicName);
    }

    private static AclBinding newReadTopicAcl(String clientId, String topicName) {
        return new AclBinding(
                clientId,
                AclPermission.ALLOW,
                AclOperation.READ,
                AclPatternType.LITERAL,
                AclResourceType.TOPIC,
                topicName);
    }

    private static AclBinding newReadAllGroupsAcl(String clientId) {
        return new AclBinding(
                clientId,
                AclPermission.ALLOW,
                AclOperation.READ,
                AclPatternType.LITERAL,
                AclResourceType.GROUP,
                "*");
    }

    private static class Context {
        private final String topicName;
        private final String serviceAccountName;
        private final Topic topic;
        private final ServiceAccount serviceAccount;
        private final List<Throwable> failures;
        private final String reason;

        private Context(String topicName, String serviceAccountName, Topic topic, ServiceAccount serviceAccount, List<Throwable> failures, String reason) {
            this.topicName = topicName;
            this.serviceAccountName = serviceAccountName;
            this.topic = topic;
            this.serviceAccount = serviceAccount;
            this.failures = failures;
            this.reason = reason;
        }

        public String getTopicName() {
            return topicName;
        }

        public String getServiceAccountName() {
            return serviceAccountName;
        }

        public Topic getTopic() {
            return topic;
        }

        public ServiceAccount getServiceAccount() {
            return serviceAccount;
        }

        public List<Throwable> getFailures() {
            return failures;
        }

        public String getReason() {
            return reason;
        }

        public boolean hasFailures() {
            return failures != null && !failures.isEmpty();
        }

        public Context withTopic(Topic topic) {
            return new Context(topicName, serviceAccountName, topic, serviceAccount, failures, reason);
        }

        public Context withServiceAccount(ServiceAccount serviceAccount) {
            return new Context(topicName, serviceAccountName, topic, serviceAccount, failures, reason);
        }

        public Context withFailure(Throwable failure) {
            return withFailure(failure, null);
        }

        public Context withFailure(Throwable failure, String reason) {
            List<Throwable> newFailures = new ArrayList<>(failures != null ? failures.size() + 1 : 1);
            if (failures != null) {
                newFailures.addAll(failures);
            }
            newFailures.add(failure);
            String newReason = this.reason == null ? reason : this.reason;
            return new Context(topicName, serviceAccountName, topic, serviceAccount, Collections.unmodifiableList(newFailures), newReason);
        }

        public static Context build(String topicName, String serviceAccountName) {
            return new Context(topicName, serviceAccountName, null, null, null, null);
        }
    }
}
