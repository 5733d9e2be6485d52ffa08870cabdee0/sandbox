package com.redhat.service.smartevents.manager.v2.vault;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.models.EventBridgeSecret;
import com.redhat.service.smartevents.infra.v2.api.V2;
import com.redhat.service.smartevents.infra.v2.api.exceptions.definitions.platform.VaultException;
import com.redhat.service.smartevents.manager.core.vault.VaultService;
import com.redhat.service.smartevents.test.resource.AWSLocalStackResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
@QuarkusTestResource(value = AWSLocalStackResource.class, restrictToAnnotatedClass = true)
public class AWSVaultServiceImplTest {

    private static final String KEY = "key";
    private static final String VALUE = "value";

    @V2
    @Inject
    VaultService vaultService;

    @Test
    public void secretIsStoredAndRetrieved() {
        String id = "secretIsStoredAndRetrieved";
        vaultService.createOrReplace(new EventBridgeSecret(id).value(KEY, VALUE)).await().indefinitely();
        String retrieved = vaultService.get(id).await().indefinitely().getValues().get(KEY);
        assertThat(retrieved).isEqualTo(VALUE);
    }

    @Test
    public void secretIsReplaced() {
        String id = "secretIsReplaced";
        String newKey = "new-key";
        String newValue = "new-value";
        vaultService.createOrReplace(new EventBridgeSecret(id).value(KEY, VALUE)).await().indefinitely();
        vaultService.createOrReplace(new EventBridgeSecret(id).value(newKey, newValue)).await().indefinitely();
        String retrieved = vaultService.get(id).await().indefinitely().getValues().get(newKey);
        assertThat(retrieved).isEqualTo(newValue);
    }

    @Test
    public void secretIsDeleted() {
        String id = "secretIsDeleted";
        vaultService.createOrReplace(new EventBridgeSecret(id).value(KEY, VALUE)).await().indefinitely();
        vaultService.delete(id).await().indefinitely();
        assertThatExceptionOfType(VaultException.class).isThrownBy(() -> vaultService.get(id).await().indefinitely());
    }

    @Test
    public void unexistingSecretThrowsException() {
        String id = "unexistingSecretThrowsException";
        assertThatExceptionOfType(VaultException.class).isThrownBy(() -> vaultService.get(id).await().indefinitely());
    }

    @Test
    public void deleteUnexistingSecretThrowsException() {
        String id = "unexistingSecretThrowsException";
        assertThatExceptionOfType(VaultException.class).isThrownBy(() -> vaultService.delete(id).await().indefinitely());
    }
}
