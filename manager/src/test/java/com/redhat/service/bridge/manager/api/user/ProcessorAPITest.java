package com.redhat.service.bridge.manager.api.user;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.dto.BridgeDTO;
import com.redhat.service.bridge.infra.dto.BridgeStatus;
import com.redhat.service.bridge.manager.TestConstants;
import com.redhat.service.bridge.manager.api.models.requests.BridgeRequest;
import com.redhat.service.bridge.manager.api.models.requests.ProcessorRequest;
import com.redhat.service.bridge.manager.api.models.responses.BridgeResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorListResponse;
import com.redhat.service.bridge.manager.api.models.responses.ProcessorResponse;
import com.redhat.service.bridge.manager.utils.DatabaseManagerUtils;
import com.redhat.service.bridge.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;

@QuarkusTest
public class ProcessorAPITest {

    @Inject
    DatabaseManagerUtils databaseManagerUtils;

    private BridgeResponse createBridge() {
        BridgeRequest r = new BridgeRequest(TestConstants.DEFAULT_BRIDGE_NAME);
        BridgeResponse bridgeResponse = TestUtils.createBridge(r).as(BridgeResponse.class);
        return bridgeResponse;
    }

    private BridgeResponse createAndDeployBridge() {
        BridgeResponse bridgeResponse = createBridge();

        BridgeDTO dto = new BridgeDTO();
        dto.setId(bridgeResponse.getId());
        dto.setStatus(BridgeStatus.AVAILABLE);
        dto.setCustomerId(TestConstants.DEFAULT_CUSTOMER_ID);
        dto.setEndpoint("https://foo.bridges.redhat.com");

        Response deployment = TestUtils.updateBridge(dto);
        assertThat(deployment.getStatusCode(), equalTo(200));
        return bridgeResponse;
    }

    @BeforeEach
    public void beforeEach() {
        databaseManagerUtils.cleanDatabase();
    }

    @Test
    public void listProcessors() {

        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor")).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2")).as(ProcessorResponse.class);

        ProcessorListResponse listResponse = TestUtils.listProcessors(bridgeResponse.getId(), 0, 100).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage(), equalTo(0L));
        assertThat(listResponse.getSize(), equalTo(2L));
        assertThat(listResponse.getTotal(), equalTo(2L));

        listResponse.getItems().forEach((i) -> assertThat(i.getId(), in(asList(p.getId(), p2.getId()))));
    }

    @Test
    public void listProcessors_pageOffset() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse p = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor")).as(ProcessorResponse.class);
        ProcessorResponse p2 = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor2")).as(ProcessorResponse.class);

        ProcessorListResponse listResponse = TestUtils.listProcessors(bridgeResponse.getId(), 1, 1).as(ProcessorListResponse.class);
        assertThat(listResponse.getPage(), equalTo(1L));
        assertThat(listResponse.getSize(), equalTo(1L));
        assertThat(listResponse.getTotal(), equalTo(2L));

        assertThat(listResponse.getItems().get(0).getId(), equalTo(p2.getId()));
    }

    @Test
    public void listProcessors_bridgeDoesNotExist() {
        assertThat(TestUtils.listProcessors("doesNotExist", 0, 100).getStatusCode(), equalTo(404));
    }

    @Test
    public void getProcessor() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode(), equalTo(201));

        ProcessorResponse pr = response.as(ProcessorResponse.class);
        ProcessorResponse found = TestUtils.getProcessor(bridgeResponse.getId(), pr.getId()).as(ProcessorResponse.class);

        assertThat(found.getId(), equalTo(pr.getId()));
        assertThat(found.getBridge().getId(), equalTo(bridgeResponse.getId()));
    }

    @Test
    public void getProcessor_processorDoesNotExist() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode(), equalTo(201));

        Response found = TestUtils.getProcessor(bridgeResponse.getId(), "doesNotExist");
        assertThat(found.getStatusCode(), equalTo(404));
    }

    @Test
    public void getProcessor_bridgeDoesNotExist() {
        BridgeResponse bridgeResponse = createAndDeployBridge();

        ProcessorResponse response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor")).as(ProcessorResponse.class);

        Response found = TestUtils.getProcessor("doesNotExist", response.getId());
        assertThat(found.getStatusCode(), equalTo(404));
    }

    @Test
    public void addProcessorToBridge() {

        BridgeResponse bridgeResponse = createAndDeployBridge();

        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode(), equalTo(201));

        ProcessorResponse processorResponse = response.as(ProcessorResponse.class);
        assertThat(processorResponse.getName(), equalTo("myProcessor"));
        assertThat(processorResponse.getBridge().getId(), equalTo(bridgeResponse.getId()));
    }

    @Test
    public void addProcessorToBridge_bridgeDoesNotExist() {

        Response response = TestUtils.addProcessorToBridge("foo", new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode(), equalTo(404));
    }

    @Test
    public void addProcessorToBridge_bridgeNotInAvailableStatus() {

        BridgeResponse bridgeResponse = createBridge();
        Response response = TestUtils.addProcessorToBridge(bridgeResponse.getId(), new ProcessorRequest("myProcessor"));
        assertThat(response.getStatusCode(), equalTo(400));
    }

    @Test
    public void addProcessorToBridge_noNameSuppliedForProcessor() {
        Response response = TestUtils.addProcessorToBridge(TestConstants.DEFAULT_BRIDGE_NAME, new ProcessorRequest());
        assertThat(response.getStatusCode(), equalTo(400));
    }
}
