package com.redhat.service.smartevents.manager.dns;

public interface DnsService {

    String buildBridgeHost(String bridgeId);

    Boolean createDnsRecord(String bridgeId);

    Boolean deleteDnsRecord(String bridgeId);
}
