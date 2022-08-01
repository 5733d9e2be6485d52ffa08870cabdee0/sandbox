package com.redhat.service.smartevents.manager.dns.openshift;

import com.amazonaws.services.route53.AmazonRoute53Async;

public interface DnsConfigOpenshiftProvider {

    AmazonRoute53Async getAmazonRouteClient();

    String getSubdomain();

    String getHostedZoneId();
}
