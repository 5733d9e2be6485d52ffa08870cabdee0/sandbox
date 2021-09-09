package com.redhat.developer.manager.models;

import java.time.ZonedDateTime;

import com.redhat.developer.infra.api.APIConstants;
import com.redhat.developer.manager.TestConstants;
import org.junit.jupiter.api.Test;

import com.redhat.developer.infra.dto.BridgeStatus;
import com.redhat.developer.manager.api.models.responses.ProcessorResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ProcessorTest {

    @Test
    public void toResponse() {

        Bridge b = new Bridge();
        b.setPublishedAt(ZonedDateTime.now());
        b.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        b.setStatus(BridgeStatus.AVAILABLE);
        b.setName(TestConstants.DEFAULT_BRIDGE_NAME);
        b.setSubmittedAt(ZonedDateTime.now());
        b.setEndpoint("https://bridge.redhat.com");

        Processor p = new Processor();
        p.setName("foo");
        p.setStatus(BridgeStatus.AVAILABLE);
        p.setPublishedAt(ZonedDateTime.now());
        p.setSubmittedAt(ZonedDateTime.now());
        p.setBridge(b);

        ProcessorResponse r = p.toResponse();
        assertThat(r, is(notNullValue()));

        assertThat(r.getHref(), equalTo(APIConstants.USER_API_BASE_PATH + b.getId() + "/processors/" + p.getId()));
        assertThat(r.getName(), equalTo(p.getName()));
        assertThat(r.getStatus(), equalTo(p.getStatus()));
        assertThat(r.getId(), equalTo(p.getId()));
        assertThat(r.getSubmittedAt(), equalTo(p.getSubmittedAt()));
        assertThat(r.getPublishedAt(), equalTo(p.getPublishedAt()));
        assertThat(r.getKind(), equalTo("Processor"));
        assertThat(r.getBridge(), is(notNullValue()));
    }
}
