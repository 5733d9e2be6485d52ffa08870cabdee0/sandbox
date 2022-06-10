package com.redhat.service.smartevents.manager.dao;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryPageInfo;
import com.redhat.service.smartevents.manager.models.CloudProvider;
import com.redhat.service.smartevents.manager.models.CloudRegion;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@QuarkusTest
public class CloudProviderDAOTest {

    @Inject
    CloudProviderDAO cloudProviderDAO;

    @Test
    public void list() {
        ListResult<CloudProvider> listResult = cloudProviderDAO.list(new QueryPageInfo(0, 1));
        assertThat(listResult.getSize()).isEqualTo(1L);
        assertThat(listResult.getPage()).isEqualTo(0L);
        assertThat(listResult.getTotal()).isEqualTo(1L);

        CloudProvider cp = listResult.getItems().get(0);
        assertThat(cp.getId()).isEqualTo("aws");
        assertThat(cp.getName()).isEqualTo("aws");
        assertThat(cp.getDisplayName()).isEqualTo("Amazon Web Services");
        assertThat(cp.isEnabled()).isTrue();

        assertThat(cp.getRegions().size()).isEqualTo(1);

        CloudRegion cloudRegion = cp.getRegions().get(0);
        assertThat(cloudRegion.getId()).isEqualTo("us-east-1");
        assertThat(cloudRegion.getName()).isEqualTo("us-east-1");
        assertThat(cloudRegion.getDisplayName()).isEqualTo("US East, N. Virginia");
        assertThat(cloudRegion.isEnabled()).isTrue();
    }

    @Test
    public void list_withPagination() {
        ListResult<CloudProvider> listResult = cloudProviderDAO.list(new QueryPageInfo(1, 1));
        assertThat(listResult.getSize()).isEqualTo(0L);
        assertThat(listResult.getTotal()).isEqualTo(1L);
    }

    @Test
    public void findByCloudProviderId() {
        CloudProvider aws = cloudProviderDAO.findById("aws");
        assertThat(aws).isNotNull();
        assertThat(aws.getId()).isEqualTo("aws");
    }

    @Test
    public void findByCloudProviderId_noMatchingProvider() {
        assertThat(cloudProviderDAO.findById("gcp")).isNull();
    }

    @Test
    public void listRegionsById() {
        ListResult<CloudRegion> regions = cloudProviderDAO.listRegionsById("aws", new QueryPageInfo(0, 1));
        assertThat(regions).isNotNull();
        assertThat(regions.getSize()).isEqualTo(1L);
        assertThat(regions.getTotal()).isEqualTo(1L);
        assertThat(regions.getPage()).isEqualTo(0L);

        CloudRegion cloudRegion = regions.getItems().get(0);
        assertThat(cloudRegion.getId()).isEqualTo("us-east-1");
        assertThat(cloudRegion.getName()).isEqualTo("us-east-1");
        assertThat(cloudRegion.getDisplayName()).isEqualTo("US East, N. Virginia");
        assertThat(cloudRegion.isEnabled()).isTrue();
    }

    @Test
    public void listRegionsById_withPagination() {
        ListResult<CloudRegion> regions = cloudProviderDAO.listRegionsById("aws", new QueryPageInfo(1, 1));
        assertThat(regions).isNotNull();
        assertThat(regions.getSize()).isEqualTo(0L);
        assertThat(regions.getTotal()).isEqualTo(1L);
        assertThat(regions.getPage()).isEqualTo(1L);
    }

    @Test
    public void listRegionById_unknownCloudProvider() {
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> cloudProviderDAO.listRegionsById("gcp", new QueryPageInfo(0, 1)));
    }
}
