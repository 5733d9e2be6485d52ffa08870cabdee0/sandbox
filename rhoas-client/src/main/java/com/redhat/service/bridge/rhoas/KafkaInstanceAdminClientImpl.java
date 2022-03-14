package com.redhat.service.bridge.rhoas;

import java.time.Duration;

import com.openshift.cloud.api.kas.auth.models.AclBinding;
import com.openshift.cloud.api.kas.auth.models.AclOperationFilter;
import com.openshift.cloud.api.kas.auth.models.AclPatternTypeFilter;
import com.openshift.cloud.api.kas.auth.models.AclPermissionTypeFilter;
import com.openshift.cloud.api.kas.auth.models.AclResourceTypeFilter;
import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.Topic;

import io.quarkus.oidc.client.OidcClient;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

public class KafkaInstanceAdminClientImpl extends AbstractAppServicesClientImpl implements KafkaInstanceAdminClient {

    private final String basePath;
    private final OidcClient oidcClient;

    public KafkaInstanceAdminClientImpl(Vertx vertx, String basePath, OidcClient oidcClient) {
        super(vertx);
        this.basePath = basePath;
        this.oidcClient = oidcClient;
    }

    @Override
    public Uni<Void> createAcl(AclBinding aclBinding) {
        return aclsVoidCall(api -> api.createAcl(aclBinding));
    }

    @Override
    public Uni<Void> deleteAcl(AclBinding aclBinding) {
        return aclsVoidCall(api -> api.deleteAcls(
                AclResourceTypeFilter.fromValue(aclBinding.getResourceType().getValue()),
                aclBinding.getResourceName(),
                AclPatternTypeFilter.fromValue(aclBinding.getPatternType().getValue()),
                aclBinding.getPrincipal(),
                AclOperationFilter.fromValue(aclBinding.getOperation().getValue()),
                AclPermissionTypeFilter.fromValue(aclBinding.getPermission().getValue())));
    }

    @Override
    public Uni<Topic> getTopic(String topicName) {
        return topicsValueCall(api -> api.getTopic(topicName));
    }

    @Override
    public Uni<Topic> createTopic(NewTopicInput newTopicInput) {
        return topicsValueCall(api -> api.createTopic(newTopicInput));
    }

    @Override
    public Uni<Void> deleteTopic(String topicName) {
        return topicsVoidCall(api -> api.deleteTopic(topicName));
    }

    @Override
    protected String getAccessToken() {
        return oidcClient.getTokens().await().atMost(Duration.ofSeconds(30)).getAccessToken();
    }

    @Override
    protected String getBasePath() {
        return basePath;
    }
}
