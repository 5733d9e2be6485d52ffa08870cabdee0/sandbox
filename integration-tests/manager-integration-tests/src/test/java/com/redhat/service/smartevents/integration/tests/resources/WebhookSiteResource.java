package com.redhat.service.smartevents.integration.tests.resources;

import java.util.ArrayList;
import java.util.List;

import com.redhat.service.smartevents.integration.tests.resources.webhook.site.WebhookSiteQuerySorting;
import com.redhat.service.smartevents.integration.tests.resources.webhook.site.WebhookSiteRequest;

import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

import software.tnb.common.service.ServiceFactory;
import software.tnb.webhook.service.Webhook;
import software.tnb.webhook.validation.RequestQueryParameters;
import software.tnb.webhook.validation.RequestQueryParameters.QuerySorting;

public class WebhookSiteResource {

    public static Webhook webhook = ServiceFactory.create(Webhook.class);

    // Manually triggering beforeAll and afterAll as these methods are intended to be triggered as JUnit5 Extension, however Cucumber support JUnit5 Extensions.

    @BeforeAll
    public static void beforeAll() throws Exception {
        webhook.beforeAll(null);
    }

    @AfterAll
    public static void afterAll() throws Exception {
        webhook.afterAll(null);
    }

    public static List<WebhookSiteRequest> requests(String webhookId, WebhookSiteQuerySorting webhookSiteQuerySorting) {
        QuerySorting sorting = WebhookSiteQuerySorting.NEWEST.equals(webhookSiteQuerySorting) ? QuerySorting.NEWEST : QuerySorting.OLDEST;
        RequestQueryParameters queryParameters = new RequestQueryParameters().setSorting(sorting);

        return convertToLocalModel(webhook.validation().getRequests(webhookId, queryParameters));
    }

    private static List<WebhookSiteRequest> convertToLocalModel(List<software.tnb.webhook.validation.model.WebhookSiteRequest> tnbRequests) {
        List<WebhookSiteRequest> requests = new ArrayList<>();
        for (software.tnb.webhook.validation.model.WebhookSiteRequest tnbRequest : tnbRequests) {
            WebhookSiteRequest request = new WebhookSiteRequest();
            request.setContent(tnbRequest.getContent());
            request.setCreatedAt(tnbRequest.getCreatedAt());
            request.setHeaders(tnbRequest.getHeaders());
            request.setUuid(tnbRequest.getUuid());
            requests.add(request);
        }
        return requests;
    }
}
