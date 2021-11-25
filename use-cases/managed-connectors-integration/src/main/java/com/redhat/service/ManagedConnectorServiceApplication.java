package com.redhat.service;

import java.io.ByteArrayInputStream;
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
import com.redhat.service.dto.request.ConnectorSpec;
import com.redhat.service.dto.request.ConnectorSpecKafka;
import com.redhat.service.dto.request.SlackConnector;

public class ManagedConnectorServiceApplication {

    String bearerToken;
    String baseUrl;
    String kafkaUrl;
    String serviceAccountId;
    String serviceAccountSecret;

    private String webhookUrl;

    public static void main(String[] args) throws Exception {

        if (args.length != 6) {
            System.out.println("Usage: arg <BEARER_TOKEN> " +
                                       "<MC_BASE_URL> " +
                                       "<KAFKA_URL> " +
                                       "<SERVICE_ACCOUNT_ID> " +
                                       "<SERVICE_ACCOUNT_SECRET> " +
                                       "<SLACK_WEBHOOK_URL>");
            System.exit(1);
        }

        ManagedConnectorServiceApplication managedConnectorServiceApplication = new ManagedConnectorServiceApplication();
        managedConnectorServiceApplication.bearerToken = args[0];
        managedConnectorServiceApplication.baseUrl = args[1];
        managedConnectorServiceApplication.kafkaUrl = args[2];
        managedConnectorServiceApplication.serviceAccountId = args[3];
        managedConnectorServiceApplication.serviceAccountSecret = args[4];
        managedConnectorServiceApplication.webhookUrl = args[5];
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
}