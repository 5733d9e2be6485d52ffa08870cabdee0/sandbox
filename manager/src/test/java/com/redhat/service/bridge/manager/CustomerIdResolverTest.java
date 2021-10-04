package com.redhat.service.bridge.manager;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class CustomerIdResolverTest {

    @Inject
    CustomerIdResolver customerIdResolver;

    @Test
    void testCustomerIdResolver() {
        assertThat(customerIdResolver.resolveCustomerId()).isEqualTo(TestConstants.DEFAULT_CUSTOMER_ID);
    }
}
