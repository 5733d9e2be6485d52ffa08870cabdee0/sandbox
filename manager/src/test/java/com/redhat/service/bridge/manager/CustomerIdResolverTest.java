package com.redhat.service.bridge.manager;

import java.security.Principal;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class CustomerIdResolverTest {

    @Inject
    CustomerIdResolver customerIdResolver;

    @Test
    public void testCustomerIdResolver() {
        Principal principal = () -> TestConstants.DEFAULT_CUSTOMER_ID;
        assertThat(customerIdResolver.resolveCustomerId(principal)).isEqualTo(TestConstants.DEFAULT_CUSTOMER_ID);
    }
}
