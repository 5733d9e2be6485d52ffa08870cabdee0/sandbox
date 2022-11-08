package com.redhat.service.smartevents.manager.core.persistence.dao;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryPageInfo;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudProvider;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudRegion;
import com.redhat.service.smartevents.test.resource.PostgresResource;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
// This annotation also bootstraps Quarkus into using DevServices for ALL @QuarkusTest's
@QuarkusTestResource(PostgresResource.class)
public class CloudProviderDAOTest {

    @Inject
    CloudProviderDAO cloudProviderDAO;

    @Test
    public void list() {
        ListResult<CloudProvider> listResult = cloudProviderDAO.list(new QueryPageInfo(0, 1));
        assertThat(listResult.getSize()).isEqualTo(1L);
        assertThat(listResult.getPage()).isZero();
        assertThat(listResult.getTotal()).isEqualTo(2L);

        CloudProvider cp = listResult.getItems().get(0);
        assertThat(cp.getId()).isEqualTo("aws");
        assertThat(cp.getName()).isEqualTo("aws");
        assertThat(cp.getDisplayName()).isEqualTo("Amazon Web Services");
        assertThat(cp.isEnabled()).isTrue();

        assertThat(cp.getRegions()).hasSize(2);

        CloudRegion cloudRegion = cp.getRegions().get(0);
        assertThat(cloudRegion.getName()).isEqualTo("us-east-1");
        assertThat(cloudRegion.getDisplayName()).isEqualTo("US East, N. Virginia");
        assertThat(cloudRegion.isEnabled()).isTrue();

        cloudRegion = cp.getRegions().get(1);
        assertThat(cloudRegion.getName()).isEqualTo("eu-west-1");
        assertThat(cloudRegion.getDisplayName()).isEqualTo("EU West, London");
        assertThat(cloudRegion.isEnabled()).isFalse();
    }

    @Test
    public void list_withPagination() {
        ListResult<CloudProvider> listResult = cloudProviderDAO.list(new QueryPageInfo(1, 1));
        assertThat(listResult.getSize()).isEqualTo(1L);
        assertThat(listResult.getTotal()).isEqualTo(2L);
        assertThat(listResult.getPage()).isEqualTo(1L);

        CloudProvider cp = listResult.getItems().get(0);
        assertThat(cp.getId()).isEqualTo("gcp");
        assertThat(cp.getName()).isEqualTo("gcp");
        assertThat(cp.getDisplayName()).isEqualTo("Google Compute Cloud");
        assertThat(cp.isEnabled()).isFalse();
    }

    @Test
    public void findByCloudProviderId() {
        CloudProvider aws = cloudProviderDAO.findById("aws");
        assertThat(aws).isNotNull();
        assertThat(aws.getId()).isEqualTo("aws");
    }

    @Test
    public void findByCloudProviderId_noMatchingProvider() {
        assertThat(cloudProviderDAO.findById("azure")).isNull();
    }

    @Test
    public void listRegionsById() {
        ListResult<CloudRegion> regions = cloudProviderDAO.listRegionsById("aws", new QueryPageInfo(0, 1));
        assertThat(regions).isNotNull();
        assertThat(regions.getSize()).isEqualTo(1L);
        assertThat(regions.getTotal()).isEqualTo(2L);
        assertThat(regions.getPage()).isZero();

        CloudRegion cloudRegion = regions.getItems().get(0);
        assertThat(cloudRegion.getName()).isEqualTo("us-east-1");
        assertThat(cloudRegion.getDisplayName()).isEqualTo("US East, N. Virginia");
        assertThat(cloudRegion.isEnabled()).isTrue();
    }

    @Test
    public void listRegionsById_withPagination() {
        ListResult<CloudRegion> regions = cloudProviderDAO.listRegionsById("aws", new QueryPageInfo(1, 1));
        assertThat(regions).isNotNull();
        assertThat(regions.getSize()).isEqualTo(1L);
        assertThat(regions.getTotal()).isEqualTo(2L);
        assertThat(regions.getPage()).isEqualTo(1L);

        CloudRegion cloudRegion = regions.getItems().get(0);
        assertThat(cloudRegion.getName()).isEqualTo("eu-west-1");
        assertThat(cloudRegion.getDisplayName()).isEqualTo("EU West, London");
        assertThat(cloudRegion.isEnabled()).isFalse();
    }

    @Test
    public void listRegionById_unknownCloudProvider() {
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> cloudProviderDAO.listRegionsById("unknownProvider", new QueryPageInfo(0, 1)));
    }
}
