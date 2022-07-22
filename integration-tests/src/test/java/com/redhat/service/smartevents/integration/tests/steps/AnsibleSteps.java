package com.redhat.service.smartevents.integration.tests.steps;

import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assumptions;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.integration.tests.resources.AnsibleResource;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

public class AnsibleSteps {

    private TestContext context;

    public AnsibleSteps(TestContext context) {
        this.context = context;
    }

    @Given("^Ansible instance url was set$")
    public void checkAnsibleUrlWasSet() {
        final String ansibleEndpoint = System.getProperty("ansible.endpoint");
        Assumptions.assumeTrue(ansibleEndpoint != null);
        Assumptions.assumeTrue(ansibleEndpoint.length() > 0);
    }

    @And("^Job template (.*) exists$")
    public void checkAnsibleJobTemplateExists(String jobTemplateName) {
        Assertions.assertThat(AnsibleResource.jobTemplateExists(jobTemplateName)).isTrue();
        context.setTestData("ansible.job.template.id", String.valueOf(AnsibleResource.jobTemplateID(jobTemplateName)));
    }

    @Then("^Ansible job with the extra parameter \"(.*)\" was run using template \"(.*)\" within (\\d+) (?:minute|minutes)$")
    public void jobWithExtraParameterWasRun(String parameter, String jobTemplateName, int timeoutMinutes) {
        final String parameterResolved = ContextResolver.resolveWithScenarioContext(context, parameter);
        final int jobTemplateId = AnsibleResource.jobTemplateID(jobTemplateName);

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    String jobs = AnsibleResource.jobs(jobTemplateId, 2, 1000);
                    Assertions.assertThat(jobs.contains(parameterResolved)).isTrue();
                });
    }
}
