package com.redhat.service.bridge.manager;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CustomerIdResolverTest {

    @Inject
    CustomerIdResolver customerIdResolver;

    @Test
    public void testCustomerIdResolver() {
        Assertions.assertEquals(TestConstants.DEFAULT_CUSTOMER_ID, customerIdResolver.resolveCustomerId());
    }
}
