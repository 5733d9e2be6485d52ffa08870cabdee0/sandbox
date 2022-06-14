package com.redhat.service.smartevents.manager.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.exceptions.definitions.platform.DeserializationException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.QueryPageInfo;
import com.redhat.service.smartevents.manager.config.ConfigurationLoader;
import com.redhat.service.smartevents.manager.models.CloudProvider;
import com.redhat.service.smartevents.manager.models.CloudRegion;

@ApplicationScoped
public class CloudProviderDAO {

    private static final String CLOUD_PROVIDERS = "cloud_providers.json";

    @Inject
    ConfigurationLoader configurationLoader;

    @Inject
    ObjectMapper objectMapper;

    private Map<String, CloudProvider> cloudProvidersById;

    private List<CloudProvider> cloudProviders;

    @PostConstruct
    void init() {

        InputStream in = configurationLoader.getConfigurationFileAsStream(CLOUD_PROVIDERS);
        try {
            this.cloudProviders = objectMapper.readValue(in, new TypeReference<>() {
            });
            this.cloudProvidersById = this.cloudProviders.stream().collect(Collectors.toMap(CloudProvider::getId, Function.identity()));
        } catch (IOException e) {
            throw new DeserializationException("Failed to parse '" + CLOUD_PROVIDERS + "'.", e);
        }
    }

    private <T> ListResult<T> buildListResult(List<T> entityList, QueryPageInfo queryPageInfo) {
        int start = queryPageInfo.getPageNumber() * queryPageInfo.getPageSize();
        List<T> response = start >= entityList.size() ? Collections.emptyList() : entityList.subList(start, Math.min(start + queryPageInfo.getPageSize(), entityList.size()));
        return new ListResult<>(response, queryPageInfo.getPageNumber(), entityList.size());
    }

    public ListResult<CloudRegion> listRegionsById(String cloudProviderId, QueryPageInfo queryPageInfo) {
        CloudProvider cp = findById(cloudProviderId);
        if (cp == null) {
            throw new ItemNotFoundException("Cloud Provider with id '" + cloudProviderId + "' does not exist");
        }

        return buildListResult(cp.getRegions(), queryPageInfo);
    }

    public ListResult<CloudProvider> list(QueryPageInfo queryInfo) {
        return buildListResult(cloudProviders, queryInfo);
    }

    public CloudProvider findById(String cloudProviderId) {
        return cloudProvidersById.get(cloudProviderId);
    }
}
