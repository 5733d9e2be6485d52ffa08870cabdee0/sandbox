package com.redhat.service.bridge.manager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.service.bridge.manager.models.TopicAndServiceAccount;
import com.redhat.service.bridge.rhoas.KafkaInstanceAdminClient;
import com.redhat.service.bridge.rhoas.KafkasMgmtV1Client;
import com.redhat.service.bridge.rhoas.dto.AclBinding;
import com.redhat.service.bridge.rhoas.dto.AclOperation;
import com.redhat.service.bridge.rhoas.dto.AclPatternType;
import com.redhat.service.bridge.rhoas.dto.AclPermission;
import com.redhat.service.bridge.rhoas.dto.AclResourceType;
import com.redhat.service.bridge.rhoas.dto.ServiceAccount;
import com.redhat.service.bridge.rhoas.dto.ServiceAccountRequest;
import com.redhat.service.bridge.rhoas.dto.Topic;
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
    public Uni<TopicAndServiceAccount> createTopicAndServiceAccount(String topicName, String serviceAccountName) {
        LOG.info("Started creation of topic={} and serviceAccount={}...", topicName, serviceAccountName);
        return Uni.createFrom().item(() -> new Context(topicName, serviceAccountName))
                .onItem().transformToUni(this::createTopic)
                .onItem().transformToUni(this::createServiceAccount)
                .onItem().transformToUni(this::configureServiceAccountACLs)
                .onItem().invoke(ctx -> LOG.info("Creation completed of topic={} and serviceAccount={}...", ctx.getTopicName(), ctx.getServiceAccountName()))
                .onItem().transform(context -> new TopicAndServiceAccount(context.getTopic(), context.getServiceAccount()));
    }

    private Uni<Context> createTopic(Context ctx) {
        return instanceClient.createTopic(new TopicRequest(ctx.getTopicName()))
                .onItem().invoke(t -> LOG.info("Created topic {}", t.getName()))
                .onFailure().invoke(e -> LOG.error("Error when creating topic", e))
                .onItem().transform(ctx::withTopic);
    }

    private Uni<Context> createServiceAccount(Context ctx) {
        // TODO: add recovery on failure
        return mgmtClient.createServiceAccount(new ServiceAccountRequest(ctx.getServiceAccountName()))
                .onItem().invoke(sa -> LOG.info("Created service account id={}, clientId={}, clientSecret={}", sa.getId(), sa.getClientId(), sa.getClientSecret()))
                .onFailure().invoke(e -> LOG.error("Error when creating service account", e))
                .onItem().transform(ctx::withServiceAccount);
    }

    private Uni<Context> configureServiceAccountACLs(Context ctx) {
        String clientId = ctx.getServiceAccount().getClientId();
        String topicName = ctx.getTopicName();

        Uni<Response> uniDescribeTopicAcl = instanceClient.createAcl(newDescribeTopicAcl(clientId, topicName));
        Uni<Response> uniReadTopicAcl = instanceClient.createAcl(newReadTopicAcl(clientId, topicName));
        Uni<Response> uniReadAllGroupsAcl = instanceClient.createAcl(newReadAllGroupsAcl(clientId));

        // TODO: add recovery on failure
        return Uni.join().all(uniDescribeTopicAcl, uniReadTopicAcl, uniReadAllGroupsAcl)
                .andCollectFailures()
                .onItem().invoke(sa -> LOG.info("Created ACLs"))
                .onFailure().invoke(e -> LOG.error("Error when creating ACLs", e))
                .onItem().transform(l -> ctx);
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
