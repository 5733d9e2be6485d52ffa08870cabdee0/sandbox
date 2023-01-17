package com.redhat.service.smartevents.manager.core.persistence.dao;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.platform.InternalPlatformException;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ExternalUserException;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryPageInfo;
import com.redhat.service.smartevents.manager.core.config.ConfigurationLoader;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudProvider;
import com.redhat.service.smartevents.manager.core.persistence.models.CloudRegion;

public abstract class AbstractCloudProviderDAO implements CloudProviderDAO {

    private static final String CLOUD_PROVIDERS = "cloud_providers.json";

    private ConfigurationLoader configurationLoader;
    private ObjectMapper objectMapper;

    private Map<String, CloudProvider> cloudProvidersById;
    private List<CloudProvider> cloudProviders;

    public AbstractCloudProviderDAO() {
        //CDI proxy
    }

    public AbstractCloudProviderDAO(ConfigurationLoader configurationLoader, ObjectMapper objectMapper) {
        this.configurationLoader = configurationLoader;
        this.objectMapper = objectMapper;
    }

    protected void init() {
        InputStream in = configurationLoader.getConfigurationFileAsStream(CLOUD_PROVIDERS);
        try {
            this.cloudProviders = objectMapper.readValue(in, new TypeReference<>() {
            });
            this.cloudProvidersById = this.cloudProviders.stream().collect(Collectors.toMap(CloudProvider::getId, Function.identity()));
        } catch (IOException e) {
            throw getDeserializationException("Failed to parse '" + CLOUD_PROVIDERS + "'.", e);
        }
    }

    protected abstract InternalPlatformException getDeserializationException(String message, Exception e);

    private <T> ListResult<T> buildListResult(List<T> entityList, QueryPageInfo queryPageInfo) {
        int start = queryPageInfo.getPageNumber() * queryPageInfo.getPageSize();
        List<T> response = start >= entityList.size() ? Collections.emptyList() : entityList.subList(start, Math.min(start + queryPageInfo.getPageSize(), entityList.size()));
        return new ListResult<>(response, queryPageInfo.getPageNumber(), entityList.size());
    }

    @Override
    public ListResult<CloudRegion> listRegionsById(String cloudProviderId, QueryPageInfo queryPageInfo) {
        CloudProvider cp = findById(cloudProviderId);
        if (cp == null) {
            throw getItemNotFoundException("Cloud Provider with id '" + cloudProviderId + "' does not exist");
        }

        return buildListResult(cp.getRegions(), queryPageInfo);
    }

    protected abstract ExternalUserException getItemNotFoundException(String message);

    @Override
    public ListResult<CloudProvider> list(QueryPageInfo queryInfo) {
        return buildListResult(cloudProviders, queryInfo);
    }

    @Override
    public CloudProvider findById(String cloudProviderId) {
        return cloudProvidersById.get(cloudProviderId);
    }
}
