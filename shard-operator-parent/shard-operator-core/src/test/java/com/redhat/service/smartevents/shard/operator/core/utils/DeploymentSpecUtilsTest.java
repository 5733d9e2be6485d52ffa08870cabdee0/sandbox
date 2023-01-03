package com.redhat.service.smartevents.shard.operator.core.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.shard.operator.core.providers.TestResourceLoader;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.apps.Deployment;

public class DeploymentSpecUtilsTest {
    @Test
    public void TestIsDeploymentEqual() {
        TestResourceLoader tl = new TestResourceLoader();
        Deployment expectedDeployment = tl.loadTestDeployment();
        Deployment existingDeployment = tl.loadTestDeployment();
        Assertions.assertThat(DeploymentSpecUtils.isDeploymentEqual(expectedDeployment, existingDeployment)).isTrue();

    }

    @Test
    public void TestIsDeploymentNotEqual_label_differs() {
        TestResourceLoader tl = new TestResourceLoader();
        Deployment expectedDeployment = tl.loadTestDeployment();
        Deployment existingDeployment = tl.loadTestDeployment();
        existingDeployment.getSpec().getSelector().getMatchLabels().remove("labelkey2");
        Assertions.assertThat(DeploymentSpecUtils.isDeploymentEqual(expectedDeployment, existingDeployment)).isFalse();

    }

    @Test
    public void TestIsDeploymentEqual_env_differs() {
        TestResourceLoader tl = new TestResourceLoader();
        Deployment expectedDeployment = tl.loadTestDeployment();
        Deployment existingDeployment = tl.loadTestDeployment();
        EnvVar envVar = new EnvVar();
        envVar.setName("key1");
        envVar.setValue("value1");
        existingDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv().add(envVar);
        Assertions.assertThat(DeploymentSpecUtils.isDeploymentEqual(expectedDeployment, existingDeployment)).isFalse();

    }

    @Test
    public void TestIsDeploymentEqual_Image_differs() {
        TestResourceLoader tl = new TestResourceLoader();
        Deployment expectedDeployment = tl.loadTestDeployment();
        Deployment existingDeployment = tl.loadTestDeployment();
        existingDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).setImage("test-image:1.0");
        Assertions.assertThat(DeploymentSpecUtils.isDeploymentEqual(expectedDeployment, existingDeployment)).isTrue();

    }

    @Test
    public void TestIsDeploymentEqual_Progressdeadline_differs() {
        TestResourceLoader tl = new TestResourceLoader();
        Deployment expectedDeployment = tl.loadTestDeployment();
        Deployment existingDeployment = tl.loadTestDeployment();
        existingDeployment.getSpec().getProgressDeadlineSeconds();
        Assertions.assertThat(DeploymentSpecUtils.isDeploymentEqual(expectedDeployment, existingDeployment)).isTrue();

    }

}
