package com.redhat.service.smartevents.manager.core.config;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudProvider;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudRegion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class ConfigurationLoaderTest {

    @TempDir
    File externalConfigDirectory;

    ConfigurationLoader configurationLoader;

    ObjectMapper objectMapper;

    @BeforeEach
    public void beforeEach() {
        configurationLoader = new ConfigurationLoader(externalConfigDirectory);
        objectMapper = new ObjectMapper();
    }

    @Test
    public void getConfigurationFileAsStream_externalDirectoryOverridesClasspath() throws Exception {
        CloudRegion region = new CloudRegion("york", "York Region", true);
        CloudProvider gcp = new CloudProvider("gcp", "gcp", "Google Compute Cloud", true, List.of(region));
        File externalConfigurationFile = new File(externalConfigDirectory, "cloud_providers.json");
        objectMapper.writeValue(externalConfigurationFile, List.of(gcp));

        InputStream in = configurationLoader.getConfigurationFileAsStream("cloud_providers.json");
        List<CloudProvider> loadedProviders = objectMapper.readValue(in, new TypeReference<>() {
        });

        assertThat(loadedProviders).hasSize(1);

        CloudProvider loadedProvider = loadedProviders.get(0);
        assertThat(loadedProvider.getId()).isEqualTo(gcp.getId());
        assertThat(loadedProvider.getName()).isEqualTo(gcp.getName());
        assertThat(loadedProvider.getDisplayName()).isEqualTo(gcp.getDisplayName());
        assertThat(loadedProvider.isEnabled()).isEqualTo(gcp.isEnabled());
        assertThat(loadedProvider.getRegions()).hasSize(1);

        CloudRegion loadedRegion = loadedProvider.getRegions().get(0);
        assertThat(loadedRegion.getName()).isEqualTo(region.getName());
        assertThat(loadedRegion.getDisplayName()).isEqualTo(region.getDisplayName());
        assertThat(loadedRegion.isEnabled()).isEqualTo(region.isEnabled());
    }

    @Test
    public void getConfigurationFileAsStream_resourceFromClasspath() throws Exception {
        InputStream in = configurationLoader.getConfigurationFileAsStream("cloud_providers.json");
        assertThat(in).isNotNull();

        List<CloudProvider> loadedProviders = objectMapper.readValue(in, new TypeReference<>() {
        });

        CloudProvider loadedProvider = loadedProviders.get(0);
        assertThat(loadedProvider.getId()).isEqualTo("aws");
        assertThat(loadedProvider.getName()).isEqualTo("aws");
        assertThat(loadedProvider.getDisplayName()).isEqualTo("Amazon Web Services");
        assertThat(loadedProvider.isEnabled()).isTrue();
        assertThat(loadedProvider.getRegions()).hasSize(2);

        CloudRegion loadedRegion = loadedProvider.getRegions().get(0);
        assertThat(loadedRegion.getName()).isEqualTo("us-east-1");
        assertThat(loadedRegion.getDisplayName()).isEqualTo("US East, N. Virginia");
        assertThat(loadedRegion.isEnabled()).isTrue();

        loadedRegion = loadedProvider.getRegions().get(1);
        assertThat(loadedRegion.getName()).isEqualTo("eu-west-1");
        assertThat(loadedRegion.getDisplayName()).isEqualTo("EU West, London");
        assertThat(loadedRegion.isEnabled()).isFalse();
    }

    @Test
    public void getConfigurationFileAsStream_resourceNotFound() {
        assertThatExceptionOfType(ConfigurationLoadException.class).isThrownBy(() -> configurationLoader.getConfigurationFileAsStream("foo.txt"));
    }
}

