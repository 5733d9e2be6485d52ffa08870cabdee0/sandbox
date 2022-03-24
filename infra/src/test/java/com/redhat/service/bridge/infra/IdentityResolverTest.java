package com.redhat.service.bridge.infra;

import java.util.Set;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.api.APIConstants;
import com.redhat.service.bridge.infra.auth.IdentityResolver;
import com.redhat.service.bridge.infra.exceptions.definitions.user.ForbiddenRequestException;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
public class IdentityResolverTest {

    private static final String CUSTOMER_ID = "kekkobar";

    @Inject
    IdentityResolver identityResolver;

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
                if (s.equals(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM)) {
                    return (T) CUSTOMER_ID;
                }
                return null;
            }
        };
        assertThat(identityResolver.getCustomerIdFromUserToken(jwt)).isEqualTo(CUSTOMER_ID);
    }

    @Test
    public void testCustomerIdResolverForServiceAccount() {
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
                if (s.equals(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)) {
                    return (T) CUSTOMER_ID;
                }
                return null;
            }
        };
        assertThat(identityResolver.getCustomerIdFromServiceAccountToken(jwt)).isEqualTo(CUSTOMER_ID);
    }

    @Test
    public void testNotValidUserToken() {
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
                return null;
            }
        };
        assertThatThrownBy(() -> identityResolver.getCustomerIdFromUserToken(jwt)).isInstanceOf(ForbiddenRequestException.class);
    }

    @Test
    public void testNotValidServiceAccountToken() {
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
                return null;
            }
        };
        assertThatThrownBy(() -> identityResolver.getCustomerIdFromServiceAccountToken(jwt)).isInstanceOf(ForbiddenRequestException.class);
    }
}
