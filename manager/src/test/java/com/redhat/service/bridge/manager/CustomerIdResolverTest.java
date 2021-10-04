package com.redhat.service.bridge.manager;

import java.security.Principal;

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
        Principal principal = () -> TestConstants.DEFAULT_CUSTOMER_ID;
        Assertions.assertEquals(TestConstants.DEFAULT_CUSTOMER_ID, customerIdResolver.resolveCustomerId(principal));
    }
}
