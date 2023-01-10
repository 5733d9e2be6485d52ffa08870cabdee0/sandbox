package com.redhat.service.smartevents.shard.operator.core.observability;

import java.util.NoSuchElementException;
import java.util.Objects;

import com.redhat.service.smartevents.shard.operator.core.providers.TemplateProvider;

import io.fabric8.kubernetes.client.KubernetesClient;

class ObservabilitySetupServiceForTests extends ObservabilitySetupService {

    ObservabilitySetupServiceForTests(KubernetesClient kubernetesClient, TemplateProvider templateProvider) {
        super.kubernetesClient = kubernetesClient;
        super.templateProvider = templateProvider;
    }

    void setEnabled(boolean enabled) {
        super.enabled = enabled;
    }

    void setName(String name) {
        super.name = name;
    }

    void setNamespace(String namespace) {
        super.namespace = namespace;
    }

    void setAccessToken(String accessToken) {
        super.accessToken = accessToken;
    }

    void setRepository(String repository) {
        super.repository = repository;
    }

    void setChannel(String channel) {
        super.channel = channel;
    }

    void setTag(String tag) {
        super.tag = tag;
    }

    @Override
    protected String getName() {
        if (Objects.isNull(super.name)) {
            throw new NoSuchElementException();
        }
        return super.name;
    }

    @Override
    protected String getNamespace() {
        if (Objects.isNull(super.namespace)) {
            throw new NoSuchElementException();
        }
        return super.namespace;
    }

    @Override
    protected String getAccessToken() {
        if (Objects.isNull(super.accessToken)) {
            throw new NoSuchElementException();
        }
        return super.accessToken;
    }

    @Override
    protected String getRepository() {
        if (Objects.isNull(super.repository)) {
            throw new NoSuchElementException();
        }
        return super.repository;
    }

    @Override
    protected String getChannel() {
        if (Objects.isNull(super.channel)) {
            throw new NoSuchElementException();
        }
        return super.channel;
    }

    @Override
    protected String getTag() {
        if (Objects.isNull(super.tag)) {
            throw new NoSuchElementException();
        }
        return super.tag;
    }
}
