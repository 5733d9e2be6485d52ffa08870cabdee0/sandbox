package com.redhat.service.smartevents.manager.dns;

public interface DnsService {

    String buildBridgeEndpoint(String bridgeId, String customerId);

    Boolean createDnsRecord(String bridgeId);

    Boolean deleteDnsRecord(String bridgeId);
}
