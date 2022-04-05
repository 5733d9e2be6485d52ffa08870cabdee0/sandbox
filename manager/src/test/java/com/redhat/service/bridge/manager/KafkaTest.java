package com.redhat.service.bridge.manager;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.openshift.cloud.api.kas.auth.TopicsApi;
import com.openshift.cloud.api.kas.auth.invoker.ApiClient;
import com.openshift.cloud.api.kas.auth.invoker.ApiException;
import com.openshift.cloud.api.kas.auth.invoker.Configuration;
import com.openshift.cloud.api.kas.auth.invoker.auth.OAuth;
import com.openshift.cloud.api.kas.auth.models.NewTopicInput;
import com.openshift.cloud.api.kas.auth.models.TopicSettings;

import static io.restassured.RestAssured.given;

public class KafkaTest {

    @Test
    public void testCreateExample() throws InterruptedException, ApiException {

        NewTopicInput topic = new NewTopicInput()
                .name("jrota-test")
                .settings(new TopicSettings().numPartitions(1));
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://admin-server-rhose-loca-c--o--mmk-hbcp-irvga.bf2.kafka.rhcloud.com:443/rest");

        TopicsApi apiInstance = new TopicsApi(defaultClient);

        System.out.println("First shot");
        setToken(defaultClient, getAccessToken());
        apiInstance.deleteTopic("jrota-test");
    }

    private void setToken(ApiClient apiClient, String token) {
        System.out.println(token);
        OAuth oauth = (OAuth) apiClient.getAuthentication("Bearer");
        oauth.setAccessToken(token);
    }

    private String getAccessToken() {
        return given().param("grant_type", "client_credentials")
                .param("client_secret", "1e738ef8-fbd7-4a29-adb4-1362264b36ea")
                .param("client_id", "srvc-acct-df06dd9b-e7d4-4e25-b257-f7a96f24ec3e")
                .param("scope", "email")
                .when()
                .post("https://identity.api.openshift.com/auth/realms/rhoas/protocol/openid-connect/token")
                .as(AccessTokenResponse.class)
                .getToken();
    }

    public static class AccessTokenResponse {

        @JsonProperty("access_token")
        protected String token;
        @JsonProperty("expires_in")
        protected long expiresIn;
        @JsonProperty("refresh_expires_in")
        protected long refreshExpiresIn;
        @JsonProperty("refresh_token")
        protected String refreshToken;
        @JsonProperty("token_type")
        protected String tokenType;
        @JsonProperty("id_token")
        protected String idToken;
        @JsonProperty("not-before-policy")
        protected int notBeforePolicy;
        @JsonProperty("session_state")
        protected String sessionState;
        protected Map<String, Object> otherClaims = new HashMap<>();
        @JsonProperty("scope")
        protected String scope;
        @JsonProperty("error")
        protected String error;
        @JsonProperty("error_description")
        protected String errorDescription;
        @JsonProperty("error_uri")
        protected String errorUri;

        public AccessTokenResponse() {
        }

        public String getToken() {
            return this.token;
        }
    }
}
