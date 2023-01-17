package com.redhat.service.smartevents.manager.v1.vault;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.v1.api.V1;
import com.redhat.service.smartevents.infra.v1.api.exceptions.definitions.platform.VaultException;
import com.redhat.service.smartevents.manager.core.vault.AbstractAWSVaultServiceImpl;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerAsyncClient;

@V1
@ApplicationScoped
public class AWSVaultServiceImpl extends AbstractAWSVaultServiceImpl {

    public AWSVaultServiceImpl() {
        //CDI proxy
    }

    @Inject
    public AWSVaultServiceImpl(SecretsManagerAsyncClient asyncClient) {
        super(asyncClient);
    }

    @Override
    protected InternalPlatformException getVaultException(String message, Throwable e) {
        return new VaultException(message, e);
    }
}
