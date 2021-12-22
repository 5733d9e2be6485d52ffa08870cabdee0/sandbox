package com.redhat.service.bridge.rhoas;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.openshift.cloud.api.kas.auth.models.AclBinding;
import com.openshift.cloud.api.kas.auth.models.AclOperationFilter;
import com.openshift.cloud.api.kas.auth.models.AclPatternTypeFilter;
import com.openshift.cloud.api.kas.auth.models.AclPermissionTypeFilter;
import com.openshift.cloud.api.kas.auth.models.AclResourceTypeFilter;
import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.Topic;

import io.quarkus.oidc.client.NamedOidcClient;
import io.quarkus.oidc.client.Tokens;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;

@ApplicationScoped
public class KafkaInstanceAdminClientImpl extends AbstractAppServicesClientImpl implements KafkaInstanceAdminClient {

    @ConfigProperty(name = "event-bridge.rhoas.instance-api.host")
    String basePath;

    @Inject
    Vertx vertx;

    @Inject
    @NamedOidcClient("mas-sso")
    Tokens tokens;

    @PostConstruct
    void onPostConstruct() {
        init(vertx);
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
    public Uni<Topic> createTopic(NewTopicInput newTopicInput) {
        return topicsValueCall(api -> api.createTopic(newTopicInput));
    }

    @Override
    public Uni<Void> deleteTopic(String topicName) {
        return topicsVoidCall(api -> api.deleteTopic(topicName));
    }

    @Override
    protected String getAccessToken() {
        return tokens.getAccessToken();
    }

    @Override
    protected String getBasePath() {
        return basePath;
    }
}
