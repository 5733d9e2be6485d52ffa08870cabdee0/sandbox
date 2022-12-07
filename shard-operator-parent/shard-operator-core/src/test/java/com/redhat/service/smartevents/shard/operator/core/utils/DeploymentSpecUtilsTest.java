package com.redhat.service.smartevents.shard.operator.core.utils;

import com.redhat.service.smartevents.shard.operator.core.TemplateProvider;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeploymentSpecUtilsTest {
    @Test
    public void TestIsDeploymentEqual(){
        Deployment expectedDeployment = TemplateProvider.loadTestDeployment();
        Deployment existingDeployment = TemplateProvider.loadTestDeployment();
        boolean result=DeploymentSpecUtils.isDeploymentEqual(expectedDeployment,existingDeployment);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void TestIsDeploymentNotEqual(){
        Deployment expectedDeployment = TemplateProvider.loadTestDeployment();
        Deployment existingDeployment = TemplateProvider.loadTestDeployment();
        existingDeployment.getSpec().getSelector().getMatchLabels().remove("labelkey2");
        boolean result=DeploymentSpecUtils.isDeploymentEqual(expectedDeployment,existingDeployment);
        Assertions.assertThat(result).isFalse();
    }

}
