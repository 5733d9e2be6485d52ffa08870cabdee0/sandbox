package com.redhat.service.bridge.manager.api.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.models.dto.BridgeDTO;
import com.redhat.service.bridge.manager.RhoasService;
import com.redhat.service.bridge.manager.ShardService;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.WorkerSchedulerProfile;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;
import io.restassured.common.mapper.TypeRef;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(WorkerSchedulerProfile.class)
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
        // Since the tests are using the user's api as well as the shard api we craft a token that is valid for both.
        when(jwt.getClaim(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.SHARD_ID);
        when(jwt.containsClaim(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM)).thenReturn(true);
        when(jwt.getClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(TestConstants.SHARD_ID);
        when(jwt.containsClaim(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)).thenReturn(true);

        // Authorize all
        when(shardService.isAuthorizedShard(any(String.class))).thenReturn(true);

        // Always assign to the default shard id
        when(shardService.getAssignedShardId(any(String.class))).thenReturn(TestConstants.SHARD_ID);
    }

    /**
     * This test needs to be in a separated class since for the current implementation the ShardService fetches
     * the authorized shards at startup. We inject a mock for this scenario.
     */
    @Test
    @TestSecurity(user = "knative")
    public void testShardSegmentation() {
        // the bridge gets assigned to the default shard
        TestUtils.createBridge(new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME));

        final List<BridgeDTO> bridgesToDeployForDefaultShard = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridgesToDeployForDefaultShard.clear();
            bridgesToDeployForDefaultShard.addAll(TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
            }));
            assertThat(bridgesToDeployForDefaultShard.size()).isEqualTo(1);
        });

        reset(jwt);
        when(jwt.getClaim(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM)).thenReturn("knative");

        // No bridges are assigned to the 'knative' shard
        List<BridgeDTO> bridgesToDeployForOtherShard = new ArrayList<>();
        await().atMost(5, SECONDS).untilAsserted(() -> {
            bridgesToDeployForOtherShard.clear();
            bridgesToDeployForOtherShard.addAll(TestUtils.getBridgesToDeployOrDelete().as(new TypeRef<List<BridgeDTO>>() {
            }));
            assertThat(bridgesToDeployForOtherShard.size()).isEqualTo(0);
        });
    }
}
