package com.redhat.service.smartevents;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Scanner;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.openshift.cloud.api.connector.ConnectorsApi;
import com.openshift.cloud.api.connector.invoker.ApiClient;
import com.openshift.cloud.api.connector.invoker.ApiException;
import com.openshift.cloud.api.connector.invoker.Configuration;
import com.openshift.cloud.api.connector.invoker.auth.HttpBearerAuth;
import com.openshift.cloud.api.connector.models.AddonClusterTarget;
import com.openshift.cloud.api.connector.models.Connector;
import com.openshift.cloud.api.connector.models.ConnectorAllOfMetadata;
import com.openshift.cloud.api.connector.models.KafkaConnectionSettings;
import com.redhat.service.smartevents.dto.request.ConnectorSpec;
import com.redhat.service.smartevents.dto.request.ConnectorSpecKafka;
import com.redhat.service.smartevents.dto.request.SlackConnector;
import io.vertx.core.json.JsonObject;

public class ManagedConnectorServiceApplication {

    String offlineToken;
    String bearerToken;

    String baseUrl;
    String kafkaUrl;
    String serviceAccountId;
    String serviceAccountSecret;

    String authServerUrl = "https://sso.redhat.com/auth/realms/redhat-external";
    String tokenPath = "protocol/openid-connect/token";
    String clientId = "cloud-services";

    private String webhookUrl;

    public static void main(String[] args) throws Exception {

        if (args.length != 6) {
            System.out.println("Usage: arg <OFFLINE_TOKEN> " +
                                       "<MC_BASE_URL> " +
                                       "<KAFKA_URL> " +
                                       "<SERVICE_ACCOUNT_ID> " +
                                       "<SERVICE_ACCOUNT_SECRET> " +
                                       "<SLACK_WEBHOOK_URL>");
            System.exit(1);
        }

        ManagedConnectorServiceApplication managedConnectorServiceApplication = new ManagedConnectorServiceApplication();
        managedConnectorServiceApplication.offlineToken = args[0];
        managedConnectorServiceApplication.baseUrl = args[1];
        managedConnectorServiceApplication.kafkaUrl = args[2];
        managedConnectorServiceApplication.serviceAccountId = args[3];
        managedConnectorServiceApplication.serviceAccountSecret = args[4];
        managedConnectorServiceApplication.webhookUrl = args[5];
        managedConnectorServiceApplication.bearerToken = managedConnectorServiceApplication.bearerTokenFromOfflineToken(managedConnectorServiceApplication.offlineToken);

        Connector slackConnector = managedConnectorServiceApplication.createSlackConnector();
        do {
            slackConnector = managedConnectorServiceApplication.pollSlackConnector(slackConnector);
        } while (!"ready".equals(slackConnector.getStatus()));
    }

    private Connector pollSlackConnector(Connector connector) throws InterruptedException, ApiException {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(baseUrl);

        HttpBearerAuth Bearer = (HttpBearerAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setBearerToken(bearerToken);

        ConnectorsApi connectorsAPI = createConnectorsAPI();
        Connector fetchedConnector = connectorsAPI.getConnector(connector.getId(), "");
        System.out.println(fetchedConnector.getStatus());
        Thread.sleep(5000);
        return fetchedConnector;
    }

    private Connector createSlackConnector() throws Exception {
        ConnectorsApi apiInstance = createConnectorsAPI();
        // Boolean | Perform the action in an asynchronous manner
        Connector createConnectorRequest = new Connector(); // Connector | Connector data

        ConnectorAllOfMetadata metadata = new ConnectorAllOfMetadata();
        metadata.setName("openbridge-slack-connector");
        metadata.setKafkaId("kafkaId-ignored");
        createConnectorRequest.setMetadata(metadata);

        AddonClusterTarget deploymentLocation = new AddonClusterTarget();
        deploymentLocation.setKind("addon");
        deploymentLocation.setClusterId("c4ovtrsldcav5gaeqkn0");
        createConnectorRequest.setDeploymentLocation(deploymentLocation);

        createConnectorRequest.setConnectorTypeId("slack_sink_0.1");

        KafkaConnectionSettings kafka = new KafkaConnectionSettings();
        kafka.setBootstrapServer(kafkaUrl);
        kafka.setClientId(serviceAccountId);
        kafka.setClientSecret(serviceAccountSecret);
        createConnectorRequest.setKafka(kafka);

        ConnectorSpec connectorSpec = new ConnectorSpec();

        ConnectorSpecKafka connectorSpecKafka = new ConnectorSpecKafka();
        connectorSpecKafka.setTopic("slacktopic");
        connectorSpec.setConnectorSpecKafka(connectorSpecKafka);

        SlackConnector connector = new SlackConnector();
        connector.setChannel("mc");
        connector.setWebhookUrl(webhookUrl);
        connectorSpec.setConnector(connector);

        createConnectorRequest.setConnectorSpec(connectorSpec);

        try {
            Connector connectorResult = apiInstance.createConnector(true, createConnectorRequest);
            System.out.println("Connector created: " + connectorResult);
            return connectorResult;
        } catch (WebApplicationException e) {
            Response response = e.getResponse();
            System.out.println("Error code: " + response.getStatus());

            ByteArrayInputStream arrayInputStream = (ByteArrayInputStream) response.getEntity();

            Scanner scanner = new Scanner(arrayInputStream);
            scanner.useDelimiter("\\Z");//To read all scanner content in one String
            String data = "";
            if (scanner.hasNext()) {
                data = scanner.next();
            }
            System.out.println(data);

            throw e;
        }
    }

    private ConnectorsApi createConnectorsAPI() {
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(baseUrl);

        HttpBearerAuth Bearer = (HttpBearerAuth) defaultClient.getAuthentication("Bearer");
        Bearer.setBearerToken(bearerToken);

        return new ConnectorsApi(defaultClient);
    }

    /**
     * Copied from https://github.com/redhat-developer/app-services-operator/blob/main/source/rhoas/src/main/java/com/openshift/cloud/beans/AccessTokenSecretTool.java#L117
     * This method exchanges an offline token for a new refresh token
     *
     * @param offlineToken the token from ss.redhat.com
     * @return a token to be used as a bearer token to authorize the user
     */
    private String bearerTokenFromOfflineToken(String offlineToken)  {
        try {
            HttpRequest request =
                    HttpRequest.newBuilder().uri(URI.create(authServerUrl + "/" + tokenPath))
                            .header("content-type", "application/x-www-form-urlencoded")
                            .timeout(Duration.ofMinutes(2)).POST(ofFormData("grant_type", "refresh_token",
                                                                            "client_id", clientId, "refresh_token", offlineToken))
                            .build();

            HttpClient client = HttpClient.newBuilder().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                var tokens = response.body();
                var json = new JsonObject(tokens);
                String access_token = json.getString("access_token");
                System.out.println("Access Token: " + access_token);
                return access_token;
            } else {
                var body = response.body();
                var apiError = String.format("Exchange token failed with error %s", body);

                throw new RuntimeException(String.format("Http Error Code %d\n", response.statusCode()) + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);

        }
    }

    private static HttpRequest.BodyPublisher ofFormData(String... data) {
        var builder = new StringBuilder();
        if (data.length % 2 == 1) {
            throw new IllegalArgumentException(
                    "Data must be key value pairs, but an number of data were given. ");
        }

        for (int index = 0; index < data.length; index += 2) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(data[index], StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(data[index + 1], StandardCharsets.UTF_8));
        }

        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }

}