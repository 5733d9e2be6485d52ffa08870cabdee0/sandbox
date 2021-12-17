package com.redhat.service.bridge.rhoas;

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
        return Uni.createFrom().item(() -> new Context(topicName, serviceAccountName))
                .onItem().transformToUni(this::createTopic)
                .onItem().transformToUni(this::createServiceAccount)
                .onItem().transformToUni(this::configureServiceAccountACLs)
                .onItem().invoke(ctx -> LOG.info("Creation completed of topic={} and serviceAccount={}", ctx.getTopicName(), ctx.getServiceAccountName()))
                .onItem().transform(context -> new TopicAndServiceAccount(context.getTopic(), context.getServiceAccount()));
    }

    private Uni<Context> createTopic(final Context ctx) {
        return instanceClient.createTopic(new TopicRequest(ctx.getTopicName()))
                .onItem().invoke(t -> LOG.info("Created topic {}", t.getName()))
                .onItem().transform(ctx::withTopic)
                .onFailure().invoke(e -> LOG.error("Error when creating topic", e));
    }

    private void deleteTopic(final String topicName) {
        LOG.info("Deleting topic {} to restore original state after failure", topicName);
        instanceClient.deleteTopic(topicName).subscribe().with(
                item -> LOG.info("Deleted topic {}", topicName),
                failure -> LOG.error("Error when deleting topic " + topicName, failure));
    }

    private Uni<Context> createServiceAccount(final Context ctx) {
        return mgmtClient.createServiceAccount(new ServiceAccountRequest(ctx.getServiceAccountName()))
                .onItem().invoke(sa -> LOG.info("Created service account id={}, clientId={}, clientSecret={}", sa.getId(), sa.getClientId(), sa.getClientSecret()))
                .onItem().transform(ctx::withServiceAccount)
                .onFailure().invoke(e -> {
                    LOG.error("Error when creating service account", e);
                    deleteTopic(ctx.getTopicName());
                });
    }

    private void deleteServiceAccount(final String serviceAccountId) {
        LOG.info("Deleting service account {} to restore original state after failure", serviceAccountId);
        mgmtClient.deleteServiceAccount(serviceAccountId).subscribe().with(
                item -> LOG.info("Deleted service account {}", serviceAccountId),
                failure -> LOG.error("Error when deleting service account " + serviceAccountId, failure));
    }

    private Uni<Context> configureServiceAccountACLs(final Context ctx) {
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
                .onFailure().invoke(e -> {
                    LOG.error("Error when creating ACLs", e);
                    deleteTopic(ctx.getTopicName());
                    deleteServiceAccount(ctx.getServiceAccount().getId());
                    deleteServiceAccountACLs(acls);
                });
    }

    private void deleteServiceAccountACLs(List<AclBinding> acls) {
        LOG.info("Deleting ACLs to restore original state after failure");
        Uni.join().all(acls.stream().map(this::deleteServiceAccountACL).collect(Collectors.toList()))
                .andCollectFailures()
                .subscribe().with(
                        item -> LOG.info("Deleted ACLs"),
                        failure -> LOG.error("Error when deleting ACLs", failure));
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
        private Topic topic;
        private ServiceAccount serviceAccount;

        public Context(String topicName, String serviceAccountName) {
            this.topicName = topicName;
            this.serviceAccountName = serviceAccountName;
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

        public void setTopic(Topic topic) {
            this.topic = topic;
        }

        public ServiceAccount getServiceAccount() {
            return serviceAccount;
        }

        public void setServiceAccount(ServiceAccount serviceAccount) {
            this.serviceAccount = serviceAccount;
        }

        public Context withTopic(Topic topic) {
            Context newCtx = new Context(this.getTopicName(), this.getServiceAccountName());
            newCtx.setTopic(topic);
            newCtx.setServiceAccount(this.getServiceAccount());
            return newCtx;
        }

        public Context withServiceAccount(ServiceAccount serviceAccount) {
            Context newCtx = new Context(this.getTopicName(), this.getServiceAccountName());
            newCtx.setTopic(this.getTopic());
            newCtx.setServiceAccount(serviceAccount);
            return newCtx;
        }
    }
}
