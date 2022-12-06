package com.redhat.service.smartevents.shard.operator.core.utils;

import com.redhat.service.smartevents.shard.operator.core.TemplateProvider;
import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

public class DeploymentStatusUtilsTest {

    @Test
    public void TestIsTimeoutFailure_success(){
        Deployment deployment = TemplateProvider.loadTestDeployment();
        DeploymentCondition timeoutCondition = new DeploymentCondition();
        timeoutCondition.setType("Progressing");
        timeoutCondition.setStatus("False");
        timeoutCondition.setReason("ProgressDeadlineExceeded");
        deployment.getStatus().getConditions().add(timeoutCondition);
        boolean result = DeploymentStatusUtils.isTimeoutFailure(deployment);
        Assertions.assertThat(result).isTrue();
    }

    @Test
    public void TestIsTimeoutFailure_failure(){
        Deployment deployment = TemplateProvider.loadTestDeployment();
        boolean result = DeploymentStatusUtils.isTimeoutFailure(deployment);
        Assertions.assertThat(result).isFalse();
    }

   @Test
    public void TestGetReasonAndMessageForTimeoutFailure_failure(){
       Deployment deployment = TemplateProvider.loadTestDeployment();
       boolean result = DeploymentStatusUtils.isTimeoutFailure(deployment);
       Assertions.assertThat(result).isFalse();

   }


}
