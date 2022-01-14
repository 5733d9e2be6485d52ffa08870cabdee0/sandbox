package com.redhat.service.bridge.infra;

import java.security.Principal;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.auth.CustomerIdResolver;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class CustomerIdResolverTest {

    @Inject
    CustomerIdResolver customerIdResolver;

    @Test
    public void testCustomerIdResolver() {
        String name = "kekkobar";
        Principal principal = () -> name;
        assertThat(customerIdResolver.resolveCustomerId(principal)).isEqualTo(name);
    }
}
