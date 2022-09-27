package com.redhat.service.smartevents.manager.api.v1.user;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.manager.api.v1.models.responses.CloudProviderListResponse;
import com.redhat.service.smartevents.manager.api.v1.models.responses.CloudProviderResponse;
import com.redhat.service.smartevents.manager.api.v1.models.responses.CloudRegionListResponse;
import com.redhat.service.smartevents.manager.api.v1.models.responses.CloudRegionResponse;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class CloudProviderAPITest {

    @Test
    public void listCloudProviders() {
        CloudProviderListResponse cloudProviders = given()
                .basePath(APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH)
                .contentType(ContentType.JSON)
                .when()
                .get()
                .as(CloudProviderListResponse.class);

        assertThat(cloudProviders.getItems().size()).isEqualTo(2);
        assertThat(cloudProviders.getPage()).isZero();
        assertThat(cloudProviders.getSize()).isEqualTo(2);
        assertThat(cloudProviders.getTotal()).isEqualTo(2);

        CloudProviderResponse cloudProviderResponse = cloudProviders.getItems().get(0);
        assertThat(cloudProviderResponse.getKind()).isEqualTo("CloudProvider");
        assertThat(cloudProviderResponse.getId()).isEqualTo("aws");
        assertThat(cloudProviderResponse.getName()).isEqualTo("aws");
        assertThat(cloudProviderResponse.getDisplayName()).isEqualTo("Amazon Web Services");
        assertThat(cloudProviderResponse.isEnabled()).isTrue();
        assertThat(cloudProviderResponse.getHref()).isEqualTo(APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/aws");

        cloudProviderResponse = cloudProviders.getItems().get(1);
        assertThat(cloudProviderResponse.getKind()).isEqualTo("CloudProvider");
        assertThat(cloudProviderResponse.getId()).isEqualTo("gcp");
        assertThat(cloudProviderResponse.getName()).isEqualTo("gcp");
        assertThat(cloudProviderResponse.getDisplayName()).isEqualTo("Google Compute Cloud");
        assertThat(cloudProviderResponse.isEnabled()).isFalse();
        assertThat(cloudProviderResponse.getHref()).isEqualTo(APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/gcp");
    }

    @Test
    public void getCloudProvider() {
        CloudProviderResponse cloudProvider = given()
                .basePath(APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/aws")
                .contentType(ContentType.JSON)
                .when()
                .get()
                .as(CloudProviderResponse.class);

        assertThat(cloudProvider.getKind()).isEqualTo("CloudProvider");
        assertThat(cloudProvider.getId()).isEqualTo("aws");
        assertThat(cloudProvider.getName()).isEqualTo("aws");
        assertThat(cloudProvider.getDisplayName()).isEqualTo("Amazon Web Services");
        assertThat(cloudProvider.isEnabled()).isTrue();
        assertThat(cloudProvider.getHref()).isEqualTo(APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/aws");
    }

    @Test
    public void listCloudProviderRegions() {
        CloudRegionListResponse cloudRegions = given()
                .basePath(APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/aws/regions")
                .contentType(ContentType.JSON)
                .when()
                .get()
                .as(CloudRegionListResponse.class);

        assertThat(cloudRegions.getItems().size()).isEqualTo(2);
        assertThat(cloudRegions.getPage()).isZero();
        assertThat(cloudRegions.getSize()).isEqualTo(2);
        assertThat(cloudRegions.getTotal()).isEqualTo(2);

        CloudRegionResponse cloudRegion = cloudRegions.getItems().get(0);
        assertThat(cloudRegion.getKind()).isEqualTo("CloudRegion");
        assertThat(cloudRegion.getName()).isEqualTo("us-east-1");
        assertThat(cloudRegion.getDisplayName()).isEqualTo("US East, N. Virginia");
        assertThat(cloudRegion.isEnabled()).isTrue();

        cloudRegion = cloudRegions.getItems().get(1);
        assertThat(cloudRegion.getKind()).isEqualTo("CloudRegion");
        assertThat(cloudRegion.getName()).isEqualTo("eu-west-1");
        assertThat(cloudRegion.getDisplayName()).isEqualTo("EU West, London");
        assertThat(cloudRegion.isEnabled()).isFalse();
    }

    @Test
    public void listCloudProviderRegions_unknownCloudProvider() {
        int fourOhFour = given()
                .basePath(APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/azure/regions")
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
                .basePath(APIConstants.V1_CLOUD_PROVIDERS_BASE_PATH + "/foobar")
                .contentType(ContentType.JSON)
                .when()
                .get()
                .andReturn()
                .statusCode();
        assertThat(fourOhFour).isEqualTo(404);
    }
}
