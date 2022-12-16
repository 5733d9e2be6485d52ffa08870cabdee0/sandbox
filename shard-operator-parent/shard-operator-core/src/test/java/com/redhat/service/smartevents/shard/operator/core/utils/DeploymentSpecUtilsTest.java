package com.redhat.service.smartevents.shard.operator.core.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.shard.operator.core.providers.TestResourceLoader;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public class DeploymentSpecUtilsTest {
    @Test
    public void TestIsDeploymentEqual() {
        Deployment expectedDeployment = TestResourceLoader.loadTestDeployment();
        Deployment existingDeployment = TestResourceLoader.loadTestDeployment();
        boolean result = DeploymentSpecUtils.isDeploymentEqual(expectedDeployment, existingDeployment);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void TestIsDeploymentNotEqual_label_differs() {
        Deployment expectedDeployment = TestResourceLoader.loadTestDeployment();
        Deployment existingDeployment = TestResourceLoader.loadTestDeployment();
        existingDeployment.getSpec().getSelector().getMatchLabels().remove("labelkey2");
        boolean result = DeploymentSpecUtils.isDeploymentEqual(expectedDeployment, existingDeployment);
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void TestIsDeploymentEqual_env_differs() {
        Deployment expectedDeployment = TestResourceLoader.loadTestDeployment();
        Deployment existingDeployment = TestResourceLoader.loadTestDeployment();
        EnvVar envVar = new EnvVar();
        envVar.setName("key1");
        envVar.setValue("value1");
        existingDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv().add(envVar);
        boolean result = DeploymentSpecUtils.isDeploymentEqual(expectedDeployment, existingDeployment);
        Assertions.assertThat(result).isFalse();
    }

    @Test
    public void TestIsDeploymentEqual_Image_differs() {
        Deployment expectedDeployment = TestResourceLoader.loadTestDeployment();
        Deployment existingDeployment = TestResourceLoader.loadTestDeployment();
        existingDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).setImage("test-image:1.0");
        boolean result = DeploymentSpecUtils.isDeploymentEqual(expectedDeployment, existingDeployment);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void TestIsDeploymentEqual_Progressdeadline_differs() {
        Deployment expectedDeployment = TestResourceLoader.loadTestDeployment();
        Deployment existingDeployment = TestResourceLoader.loadTestDeployment();
        existingDeployment.getSpec().getProgressDeadlineSeconds();
        boolean result = DeploymentSpecUtils.isDeploymentEqual(expectedDeployment, existingDeployment);
        Assertions.assertThat(result).isTrue();
    }

}
