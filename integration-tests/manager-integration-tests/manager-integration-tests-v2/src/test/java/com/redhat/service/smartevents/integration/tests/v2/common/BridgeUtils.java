package com.redhat.service.smartevents.integration.tests.v2.common;

import com.redhat.service.smartevents.integration.tests.context.BridgeContext;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.v2.resources.BridgeResource;

public class BridgeUtils {

    public static final String MANAGER_URL = System.getProperty("event-bridge.manager.url");
    protected static final String CLIENT_ID = System.getProperty("bridge.client.id");
    protected static final String CLIENT_SECRET = System.getProperty("bridge.client.secret");
    protected static final String OB_TOKEN = System.getenv("OB_TOKEN");
    protected static final String OPENSHIFT_OFFLINE_TOKEN = System.getenv("OPENSHIFT_OFFLINE_TOKEN");

    protected static String keycloakURL = System.getProperty("keycloak.realm.url");

    public static String getOrRetrieveBridgeEventsEndpoint(TestContext context, String testBridgeName) {
        BridgeContext bridgeContext = context.getBridge(testBridgeName);

        if (bridgeContext.getEndPoint() == null) {
            // store bridge endpoint details
            String endPoint = BridgeResource.getBridgeDetails(context.getManagerToken(), bridgeContext.getId())
                    .getEndpoint();
            // If an endpoint contains localhost without port then default port has to be
            // defined, otherwise rest-assured will use port 8080
            if (endPoint.matches("http://localhost/.*")) {
                endPoint = endPoint.replace("http://localhost/", "http://localhost:80/");
            }
            bridgeContext.setEndPoint(endPoint);
        }
        context.getScenario().log("Resolved Bridge endpoint " + bridgeContext.getEndPoint());
        return bridgeContext.getEndPoint();
    }
}
