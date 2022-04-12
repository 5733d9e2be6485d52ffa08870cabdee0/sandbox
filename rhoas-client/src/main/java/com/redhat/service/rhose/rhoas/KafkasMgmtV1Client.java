package com.redhat.service.rhose.rhoas;

import com.openshift.cloud.api.kas.models.ServiceAccount;
import com.openshift.cloud.api.kas.models.ServiceAccountRequest;

import io.smallrye.mutiny.Uni;

public interface KafkasMgmtV1Client {

    Uni<ServiceAccount> createServiceAccount(ServiceAccountRequest serviceAccountRequest);

    Uni<Void> deleteServiceAccount(String id);

}
