package com.redhat.service.smartevents.manager.api.user;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.manager.TestConstants;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorCatalogResponse;
import com.redhat.service.smartevents.manager.api.models.responses.ProcessorSchemaEntryResponse;
import com.redhat.service.smartevents.manager.utils.TestUtils;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.quarkus.test.security.TestSecurity;

import static com.redhat.service.smartevents.infra.api.APIConstants.USER_NAME_ATTRIBUTE_CLAIM;
import static com.redhat.service.smartevents.manager.TestConstants.DEFAULT_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

@QuarkusTest
public class SchemaAPITest {

    @InjectMock
    JsonWebToken jwt;

    @BeforeEach
    public void cleanUp() {
        when(jwt.getClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(DEFAULT_USER_NAME);
        when(jwt.containsClaim(USER_NAME_ATTRIBUTE_CLAIM)).thenReturn(true);
    }

    @Test
    public void testSchemasAreIncludedInCatalog() throws IOException {
        File actionsDir = new File("src/main/resources/schemas/actions/");
        File sourcesDir = new File("src/main/resources/schemas/sources/");
        List<String> actions = Arrays.stream(Objects.requireNonNull(actionsDir.listFiles())).map(File::getName).collect(Collectors.toList());
        List<String> sources = Arrays.stream(Objects.requireNonNull(sourcesDir.listFiles())).map(File::getName).collect(Collectors.toList());
        ProcessorCatalogResponse catalog = TestUtils.getProcessorsSchemaCatalog().as(ProcessorCatalogResponse.class);

        assertThat(actions).contains("catalog.json");
        assertThat(sources).contains("catalog.json");
        assertThat(catalog.getItems().stream().filter(x -> "action".equals(x.getType())).count())
                .withFailMessage("An action processor json schema file was not added to the catalog.json file.")
                .isEqualTo(actions.size() - 1);
        assertThat(catalog.getItems().stream().filter(x -> "source".equals(x.getType())).count())
                .withFailMessage("A source processor json schema file was not added to the catalog.json file.")
                .isEqualTo(sources.size() - 1);
    }

    @Test
    public void testAuthentication() {
        TestUtils.getProcessorsSchemaCatalog().then().statusCode(401);
    }

    @Test
    @TestSecurity(user = TestConstants.DEFAULT_CUSTOMER_ID)
    public void listProcessors() {
        List<String> availableActions = List.of("KafkaTopic", "SendToBridge", "Slack", "Webhook");
        List<String> availableSources = List.of("AwsS3", "AwsSqs", "Slack");
        ProcessorCatalogResponse catalog = TestUtils.getProcessorsSchemaCatalog().as(ProcessorCatalogResponse.class);

        assertThat(catalog.getItems()).isNotNull();
        assertThat(catalog.getItems().size())
                .withFailMessage("The size of the catalog does not match. If you added a new action or a new source under /resources/schemas/ please update this test")
                .isEqualTo(7);
        for (ProcessorSchemaEntryResponse entry : catalog.getItems()) {
            switch (entry.getType()) {
                case "action":
                    assertThat(availableActions).contains(entry.getName());
                    break;
                case "source":
                    assertThat(availableSources).contains(entry.getName());
                    break;
                default:
                    fail("entry type does not match 'source' nor 'action'");
            }
            assertThatNoException().isThrownBy(() -> new URI(entry.getHref())); // is a valid URI
            assertThat(entry.getHref()).contains(entry.getName()); // The href should contain the name
            assertThat(entry.getHref()).contains(".json"); // The href points to a json file
        }
    }
}
