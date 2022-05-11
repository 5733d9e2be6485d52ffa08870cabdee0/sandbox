package com.redhat.service.smartevents.infra;

import javax.inject.Inject;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.auth.IdentityResolver;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ForbiddenRequestException;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusTest
public class IdentityResolverTest {

    private static final String CUSTOMER_ID = "kekkobar";
    private static final String ORGANISATION_ID = "myOrg";

    @Inject
    IdentityResolver identityResolver;

    @Test
    public void testCustomerIdResolver() {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getClaim(eq(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM))).thenReturn(CUSTOMER_ID);
        when(jwt.containsClaim(eq(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM))).thenReturn(true);
        when(jwt.containsClaim(not(eq(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM)))).thenReturn(false);
        assertThat(identityResolver.resolve(jwt)).isEqualTo(CUSTOMER_ID);
    }

    @Test
    public void testCustomerIdResolverForServiceAccount() {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getClaim(eq(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM))).thenReturn(CUSTOMER_ID);
        when(jwt.containsClaim(eq(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM))).thenReturn(true);
        when(jwt.containsClaim(not(eq(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM)))).thenReturn(false);
        assertThat(identityResolver.resolve(jwt)).isEqualTo(CUSTOMER_ID);
    }

    @Test
    public void testValidTokenWithoutAccountIdClaims() {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.containsClaim(eq(APIConstants.ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM))).thenReturn(false);
        when(jwt.containsClaim(eq(APIConstants.ACCOUNT_ID_USER_ATTRIBUTE_CLAIM))).thenReturn(false);
        assertThatThrownBy(() -> identityResolver.resolve(jwt)).isInstanceOf(ForbiddenRequestException.class);
    }

    @Test
    public void testOrganisationIdResolver() {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.getClaim(eq(APIConstants.ORG_ID_USER_ATTRIBUTE_CLAIM))).thenReturn(ORGANISATION_ID);
        when(jwt.containsClaim(eq(APIConstants.ORG_ID_USER_ATTRIBUTE_CLAIM))).thenReturn(true);
        assertThat(identityResolver.resolveOrganisationId(jwt)).isEqualTo(ORGANISATION_ID);
    }

    @Test
    public void testValidTokenWithoutOrganisationIdClaims() {
        JsonWebToken jwt = mock(JsonWebToken.class);
        when(jwt.containsClaim(eq(APIConstants.ORG_ID_USER_ATTRIBUTE_CLAIM))).thenReturn(false);
        assertThatThrownBy(() -> identityResolver.resolve(jwt)).isInstanceOf(ForbiddenRequestException.class);
    }
}
