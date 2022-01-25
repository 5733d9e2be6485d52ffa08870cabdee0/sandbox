package com.redhat.service.bridge.manager;

import java.util.Set;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.api.APIConstants;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class CustomerIdResolverTest {

    @Inject
    CustomerIdResolver customerIdResolver;

    @Test
    public void testCustomerIdResolver() {
        JsonWebToken jwt = new JsonWebToken() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public Set<String> getClaimNames() {
                return null;
            }

            @Override
            public <T> T getClaim(String s) {
                if (s.equals(APIConstants.SUBJECT_ATTRIBUTE_CLAIM)) {
                    return (T) TestConstants.DEFAULT_CUSTOMER_ID;
                }
                return null;
            }

            @Override
            public String getSubject() {
                return TestConstants.DEFAULT_CUSTOMER_ID;
            }
        };
        assertThat(customerIdResolver.resolveCustomerId(jwt)).isEqualTo(TestConstants.DEFAULT_CUSTOMER_ID);
    }
}
