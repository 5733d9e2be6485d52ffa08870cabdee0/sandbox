package com.redhat.service.smartevents.manager.dns.openshift;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.route53.AmazonRoute53Async;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.redhat.service.smartevents.manager.ShardService;
import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.dns.DnsService;
import com.redhat.service.smartevents.manager.utils.DatabaseManagerUtils;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
public class DnsServiceOpenshiftImplTest {

    private static final String SUBDOMAIN = ".smartevents.bf2.dev";
    private static final String HOSTED_ZONE_ID = "zoneid";

    DnsConfigOpenshiftProvider dnsConfigOpenshiftProvider;

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    @Inject
    ShardService shardService;

    @BeforeEach
    public void init() {
        databaseManagerUtils.cleanUpAndInitWithDefaultShard();

        dnsConfigOpenshiftProvider = mock(DnsConfigOpenshiftProvider.class);
        when(dnsConfigOpenshiftProvider.getSubdomain()).thenReturn(SUBDOMAIN);
        when(dnsConfigOpenshiftProvider.getHostedZoneId()).thenReturn(HOSTED_ZONE_ID);
    }

    @Test
    public void testBuildBridgeHost() {
        DnsService dnsService = new DnsServiceOpenshiftImpl(shardService, dnsConfigOpenshiftProvider);
        assertThat(dnsService.buildBridgeEndpoint(TestConstants.DEFAULT_BRIDGE_ID, TestConstants.DEFAULT_CUSTOMER_ID))
                .isEqualTo("https://" + TestConstants.DEFAULT_BRIDGE_ID + SUBDOMAIN + "/ob-" + TestConstants.DEFAULT_CUSTOMER_ID + "/ob-" + TestConstants.DEFAULT_BRIDGE_ID);
    }

    @Test
    public void testSuccessfulCreation() {
        AmazonRoute53Async clientMock = mock(AmazonRoute53Async.class);
        when(clientMock.changeResourceRecordSetsAsync(any(ChangeResourceRecordSetsRequest.class))).thenReturn(CompletableFuture.completedFuture(new ChangeResourceRecordSetsResult()));
        when(dnsConfigOpenshiftProvider.getAmazonRouteClient()).thenReturn(clientMock);

        DnsService dnsService = new DnsServiceOpenshiftImpl(shardService, dnsConfigOpenshiftProvider);
        assertThat(dnsService.createDnsRecord(TestConstants.DEFAULT_BRIDGE_ID)).isTrue();

        verify(clientMock, times(1)).changeResourceRecordSetsAsync(any(ChangeResourceRecordSetsRequest.class));
    }

    @Test
    public void testFailedCreation() {
        AmazonRoute53Async clientMock = mock(AmazonRoute53Async.class);
        when(clientMock.changeResourceRecordSetsAsync(any(ChangeResourceRecordSetsRequest.class))).thenReturn(CompletableFuture.failedFuture(new RuntimeException()));
        when(dnsConfigOpenshiftProvider.getAmazonRouteClient()).thenReturn(clientMock);

        DnsService dnsService = new DnsServiceOpenshiftImpl(shardService, dnsConfigOpenshiftProvider);

        assertThatThrownBy(() -> dnsService.createDnsRecord(TestConstants.DEFAULT_BRIDGE_ID)).isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testSuccessfulDeletion() {

        AmazonRoute53Async clientMock = mock(AmazonRoute53Async.class);
        when(clientMock.changeResourceRecordSetsAsync(any(ChangeResourceRecordSetsRequest.class))).thenReturn(CompletableFuture.completedFuture(new ChangeResourceRecordSetsResult()));
        when(dnsConfigOpenshiftProvider.getAmazonRouteClient()).thenReturn(clientMock);

        DnsService dnsService = new DnsServiceOpenshiftImpl(shardService, dnsConfigOpenshiftProvider);
        assertThat(dnsService.deleteDnsRecord(TestConstants.DEFAULT_BRIDGE_ID)).isTrue();

        verify(clientMock, times(1)).changeResourceRecordSetsAsync(any(ChangeResourceRecordSetsRequest.class));
    }

    @Test
    public void testFailedDeletion() {
        AmazonRoute53Async clientMock = mock(AmazonRoute53Async.class);
        when(clientMock.changeResourceRecordSetsAsync(any(ChangeResourceRecordSetsRequest.class))).thenReturn(CompletableFuture.failedFuture(new RuntimeException()));
        when(dnsConfigOpenshiftProvider.getAmazonRouteClient()).thenReturn(clientMock);

        DnsService dnsService = new DnsServiceOpenshiftImpl(shardService, dnsConfigOpenshiftProvider);

        assertThatThrownBy(() -> dnsService.deleteDnsRecord(TestConstants.DEFAULT_BRIDGE_ID)).isInstanceOf(RuntimeException.class);
    }
}
