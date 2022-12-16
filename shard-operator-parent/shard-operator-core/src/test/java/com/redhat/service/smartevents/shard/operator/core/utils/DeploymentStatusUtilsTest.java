package com.redhat.service.smartevents.shard.operator.core.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.shard.operator.core.providers.TestResourceLoader;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;

public class DeploymentStatusUtilsTest {

    @Test
    public void TestIsTimeoutFailure_success() {
        TestResourceLoader tl = new TestResourceLoader();
        Deployment deployment = tl.loadTestDeployment();
        DeploymentCondition timeoutCondition = new DeploymentCondition();
        timeoutCondition.setType("Progressing");
        timeoutCondition.setStatus("False");
        timeoutCondition.setReason("ProgressDeadlineExceeded");
        deployment.getStatus().getConditions().add(timeoutCondition);
        Assertions.assertThat(DeploymentStatusUtils.isTimeoutFailure(deployment))
                .isTrue();
    }

    @Test
    public void TestIsTimeoutFailure_progressing_true() {
        TestResourceLoader tl = new TestResourceLoader();
        Deployment deployment = tl.loadTestDeployment();
        DeploymentCondition timeoutCondition = new DeploymentCondition();
        timeoutCondition.setType("Progressing");
        timeoutCondition.setStatus("True");
        deployment.getStatus().getConditions().add(timeoutCondition);
        Assertions.assertThat(DeploymentStatusUtils.isTimeoutFailure(deployment))
                .isFalse();
    }

    @Test
    public void TestIsTimeoutFailure_reason_mismatch() {
        TestResourceLoader tl = new TestResourceLoader();
        Deployment deployment = tl.loadTestDeployment();
        //        Deployment deployment = TestResourceLoader.loadTestDeployment();
        DeploymentCondition timeoutCondition = new DeploymentCondition();
        timeoutCondition.setType("Progressing");
        timeoutCondition.setStatus("False");
        timeoutCondition.setReason("testReason");
        deployment.getStatus().getConditions().add(timeoutCondition);
        Assertions.assertThat(DeploymentStatusUtils.isTimeoutFailure(deployment))
                .isFalse();
    }
}
