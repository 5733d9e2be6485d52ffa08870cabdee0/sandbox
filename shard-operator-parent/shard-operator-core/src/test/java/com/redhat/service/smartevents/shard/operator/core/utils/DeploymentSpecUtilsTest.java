package com.redhat.service.smartevents.shard.operator.core.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.shard.operator.core.TemplateProvider;

import io.fabric8.kubernetes.api.model.apps.Deployment;

public class DeploymentSpecUtilsTest {
    @Test
    public void TestIsDeploymentEqual() {
        Deployment expectedDeployment = TemplateProvider.loadTestDeployment();
        Deployment existingDeployment = TemplateProvider.loadTestDeployment();
        boolean result = DeploymentSpecUtils.isDeploymentEqual(expectedDeployment, existingDeployment);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void TestIsDeploymentNotEqual() {
        Deployment expectedDeployment = TemplateProvider.loadTestDeployment();
        Deployment existingDeployment = TemplateProvider.loadTestDeployment();
        existingDeployment.getSpec().getSelector().getMatchLabels().remove("labelkey2");
        boolean result = DeploymentSpecUtils.isDeploymentEqual(expectedDeployment, existingDeployment);
        Assertions.assertThat(result).isFalse();
    }

}
