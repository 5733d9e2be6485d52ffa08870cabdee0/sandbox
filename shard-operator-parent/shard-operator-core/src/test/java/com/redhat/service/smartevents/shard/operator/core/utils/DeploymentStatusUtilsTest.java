package com.redhat.service.smartevents.shard.operator.core.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.shard.operator.core.TemplateProvider;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;

public class DeploymentStatusUtilsTest {

    @Test
    public void TestIsTimeoutFailure_success() {
        Deployment deployment = TemplateProvider.loadTestDeployment();
        DeploymentCondition timeoutCondition = new DeploymentCondition();
        timeoutCondition.setType("Progressing");
        timeoutCondition.setStatus("False");
        timeoutCondition.setReason("ProgressDeadlineExceeded");
        deployment.getStatus().getConditions().add(timeoutCondition);
        Assertions.assertThat(DeploymentStatusUtils.isTimeoutFailure(deployment))
                .isTrue();
    }

    @Test
    public void TestIsTimeoutFailure_failure() {
        Deployment deployment = TemplateProvider.loadTestDeployment();
        Assertions.assertThat(DeploymentStatusUtils.isTimeoutFailure(deployment))
                .isFalse();
    }

    @Test
    public void TestGetReasonAndMessageForTimeoutFailure() {
        Deployment deployment = TemplateProvider.loadTestDeployment();
//        boolean result = DeploymentStatusUtils.isTimeoutFailure(deployment);
//        Assertions.assertThat(result).isFalse();
        Assertions.assertThat(DeploymentStatusUtils.isTimeoutFailure(deployment))
                .isFalse();

    }

}
