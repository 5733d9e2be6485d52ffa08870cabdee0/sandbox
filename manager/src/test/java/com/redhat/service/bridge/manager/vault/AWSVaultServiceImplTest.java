package com.redhat.service.bridge.manager.vault;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.exceptions.definitions.platform.VaultException;
import com.redhat.service.bridge.infra.models.EventBridgeSecret;
import com.redhat.service.bridge.test.resource.AWSLocalStackResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
@QuarkusTestResource(value = AWSLocalStackResource.class, restrictToAnnotatedClass = true)
public class AWSVaultServiceImplTest {

    private static final String KEY = "key";
    private static final String VALUE = "value";

    @Inject
    AWSVaultServiceImpl vaultService;

    @Test
    public void secretIsStoredAndRetrieved() {
        String id = "secretIsStoredAndRetrieved";
        vaultService.createOrReplace(new EventBridgeSecret(id)
                .value(KEY, VALUE));
        String retrieved = vaultService.get(id).getValues().get(KEY);
        assertThat(retrieved).isEqualTo(VALUE);
    }

    @Test
    public void secretIsReplaced() {
        String id = "secretIsReplaced";
        String newKey = "new-key";
        String newValue = "new-value";
        vaultService.createOrReplace(new EventBridgeSecret(id)
                .value(KEY, VALUE));
        vaultService.createOrReplace(new EventBridgeSecret(id)
                .value(newKey, newValue));
        String retrieved = vaultService.get(id).getValues().get(newKey);
        assertThat(retrieved).isEqualTo(newValue);
    }

    @Test
    public void secretIsDeleted() {
        String id = "secretIsDeleted";
        vaultService.createOrReplace(new EventBridgeSecret(id)
                .value(KEY, VALUE));
        vaultService.delete(id);
        assertThatExceptionOfType(VaultException.class).isThrownBy(() -> vaultService.get(id));
    }

    @Test
    public void unexistingSecretThrowsException() {
        String id = "unexistingSecretThrowsException";
        assertThatExceptionOfType(VaultException.class).isThrownBy(() -> vaultService.get(id));
    }
}
