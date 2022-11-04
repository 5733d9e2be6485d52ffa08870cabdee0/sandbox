package com.redhat.service.smartevents.manager.api.v1.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.api.v1.models.dto.BridgeDTO;
import com.redhat.service.smartevents.manager.RhoasService;
import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.api.v1.models.requests.BridgeRequest;
import com.redhat.service.smartevents.manager.persistence.v1.models.Shard;
import com.redhat.service.smartevents.manager.services.v1.ShardService;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;
import com.redhat.service.smartevents.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;

import static com.redhat.service.smartevents.infra.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_USER_NAME;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ShardBridgesSyncSegmentationAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @InjectMock
    JsonWebToken jwt;

    @InjectMock
    RhoasService rhoasServiceMock;

    @InjectMock
    ShardService shardService;

    @BeforeEach
    public void cleanUp() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();

        // Authorize all
        when(shardService.isAuthorizedShard(any(String.class))).thenReturn(true);

        // Always assign to the default shard id
        Shard shard = new Shard();
        shard.setId(TestConstants.SHARD_ID);
        shard.setRouterCanonicalHostname(TestConstants.DEFAULT_SHARD_ROUTER_CANONICAL_HOSTNAME);
        when(shardService.getAssignedShard(any(String.class))).thenReturn(shard);
    }

    /**
     * This test needs to be in a separated class since for the current implementation the ShardService fetches
     * the authorized shards at startup. We inject a mock for this scenario.
     */
    @Test
    @TestSecurity(user = "knative")
    public void testShardSegmentation() {
        // the bridge gets assigned to the default shard
        mockJwt(TestConstants.SHARD_ID);
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME, TestConstants.DEFAULT_CLOUD_PROVIDER, TestConstants.DEFAULT_REGION));

        final List<BridgeDTO> bridgesToDeployForDefaultShard = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridgesToDeployForDefaultShard.clear();
            bridgesToDeployForDefaultShard.addAll(TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
            }));
            assertThat(bridgesToDeployForDefaultShard.size()).isEqualTo(1);
        });

        mockJwt("knative");
        // No bridges are assigned to the 'knative' shard
        List<BridgeDTO> bridgesToDeployForOtherShard = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridgesToDeployForOtherShard.clear();
            bridgesToDeployForOtherShard.addAll(TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
            }));
            assertThat(bridgesToDeployForOtherShard.size()).isEqualTo(0);
        });
    }

    private void mockJwt(String shardId) {
        // Since the tests are using the user's api as well as the shard api we craft a token that is valid for both.
        reset(jwt);
        when(jwt.getClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(shardId);
        when(jwt.containsClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.DEFAULT_ORGANISATION_ID);
        when(jwt.containsClaim(APIConstants.ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(DEFAULT_USER_NAME);
        when(jwt.containsClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(true);
    }
}
