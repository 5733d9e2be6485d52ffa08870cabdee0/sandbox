package com.redhat.service.smartevents.manager.v1.api.user;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;
import com.redhat.service.smartevents.manager.core.api.models.responses.CloudProviderListResponse;
import com.redhat.service.smartevents.manager.core.api.models.responses.CloudProviderResponse;
import com.redhat.service.smartevents.manager.core.api.models.responses.CloudRegionListResponse;
import com.redhat.service.smartevents.manager.core.api.models.responses.CloudRegionResponse;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class CloudProviderAPITest {

    @Test
    public void listCloudProviders() {
        CloudProviderListResponse cloudProviders = given()
                .basePath(V1APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH)
                .contentType(ContentType.JSON)
                .when()
                .get()
                .as(CloudProviderListResponse.class);

        assertThat(cloudProviders.getItems().size()).isEqualTo(2);
        assertThat(cloudProviders.getPage()).isZero();
        assertThat(cloudProviders.getSize()).isEqualTo(2);
        assertThat(cloudProviders.getTotal()).isEqualTo(2);

        CloudProviderResponse cloudProviderResponse1 = cloudProviders.getItems().get(0);
        assertThat(cloudProviderResponse1.getKind()).isEqualTo("CloudProvider");
        assertThat(cloudProviderResponse1.getId()).isEqualTo("aws");
        assertThat(cloudProviderResponse1.getName()).isEqualTo("aws");
        assertThat(cloudProviderResponse1.getDisplayName()).isEqualTo("Amazon Web Services");
        assertThat(cloudProviderResponse1.isEnabled()).isTrue();
        assertThat(cloudProviderResponse1.getHref()).isEqualTo(V1APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/aws");

        CloudProviderResponse cloudProviderResponse2 = cloudProviders.getItems().get(1);
        assertThat(cloudProviderResponse2.getKind()).isEqualTo("CloudProvider");
        assertThat(cloudProviderResponse2.getId()).isEqualTo("gcp");
        assertThat(cloudProviderResponse2.getName()).isEqualTo("gcp");
        assertThat(cloudProviderResponse2.getDisplayName()).isEqualTo("Google Compute Cloud");
        assertThat(cloudProviderResponse2.isEnabled()).isFalse();
        assertThat(cloudProviderResponse2.getHref()).isEqualTo(V1APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/gcp");
    }

    @Test
    public void getCloudProvider() {
        CloudProviderResponse cloudProvider = given()
                .basePath(V1APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/aws")
                .contentType(ContentType.JSON)
                .when()
                .get()
                .as(CloudProviderResponse.class);

        assertThat(cloudProvider.getKind()).isEqualTo("CloudProvider");
        assertThat(cloudProvider.getId()).isEqualTo("aws");
        assertThat(cloudProvider.getName()).isEqualTo("aws");
        assertThat(cloudProvider.getDisplayName()).isEqualTo("Amazon Web Services");
        assertThat(cloudProvider.isEnabled()).isTrue();
        assertThat(cloudProvider.getHref()).isEqualTo(V1APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/aws");
    }

    @Test
    public void listCloudProviderRegions() {
        CloudRegionListResponse cloudRegions = given()
                .basePath(V1APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/aws/regions")
                .contentType(ContentType.JSON)
                .when()
                .get()
                .as(CloudRegionListResponse.class);

        assertThat(cloudRegions.getItems().size()).isEqualTo(2);
        assertThat(cloudRegions.getPage()).isZero();
        assertThat(cloudRegions.getSize()).isEqualTo(2);
        assertThat(cloudRegions.getTotal()).isEqualTo(2);

        CloudRegionResponse cloudRegion1 = cloudRegions.getItems().get(0);
        assertThat(cloudRegion1.getKind()).isEqualTo("CloudRegion");
        assertThat(cloudRegion1.getName()).isEqualTo("us-east-1");
        assertThat(cloudRegion1.getDisplayName()).isEqualTo("US East, N. Virginia");
        assertThat(cloudRegion1.isEnabled()).isTrue();

        CloudRegionResponse cloudRegion2 = cloudRegions.getItems().get(1);
        assertThat(cloudRegion1.getKind()).isEqualTo("CloudRegion");
        assertThat(cloudRegion2.getName()).isEqualTo("eu-west-1");
        assertThat(cloudRegion2.getDisplayName()).isEqualTo("EU West, London");
        assertThat(cloudRegion2.isEnabled()).isFalse();
    }

    @Test
    public void listCloudProviderRegions_unknownCloudProvider() {
        int fourOhFour = given()
                .basePath(V1APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/azure/regions")
                .contentType(ContentType.JSON)
                .when()
                .get()
                .andReturn()
                .statusCode();
        assertThat(fourOhFour).isEqualTo(404);
    }

    @Test
    public void getCloudProvider_unknownCloudProvider() {
        int fourOhFour = given()
                .basePath(V1APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/foobar")
                .contentType(ContentType.JSON)
                .when()
                .get()
                .andReturn()
                .statusCode();
        assertThat(fourOhFour).isEqualTo(404);
    }
}
