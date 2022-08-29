package com.redhat.service.smartevents.integration.tests.steps;

import java.time.Duration;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;

import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.integration.tests.resources.ansible.AnsibleResource;
import com.redhat.service.smartevents.integration.tests.resources.ansible.Job;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;

public class AnsibleSteps {

    private TestContext context;

    public AnsibleSteps(TestContext context) {
        this.context = context;
    }

    @And("^Job template (.*) exists$")
    public void checkAnsibleJobTemplateExists(String jobTemplateName) {
        Assertions.assertThat(AnsibleResource.jobTemplatesList(jobTemplateName))
                .anyMatch(s -> s.contains(jobTemplateName));
        context.setTestData("ansible.job.template.id", String.valueOf(AnsibleResource.jobTemplateID(jobTemplateName)));
    }

    @Then("^Ansible job with the extra parameter \"(.*)\" was run in (\\d+) days using template \"(.*)\" within (\\d+) (?:minute|minutes)$")
    public void jobWithExtraParameterWasRun(String parameter, int days, String jobTemplateName, int timeoutMinutes) {
        final String parameterResolved = ContextResolver.resolveWithScenarioContext(context, parameter);
        final int jobTemplateId = AnsibleResource.jobTemplateID(jobTemplateName);

        Awaitility.await()
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    List<Job> jobs = AnsibleResource.jobs(jobTemplateId, days, 1000);
                    Assertions.assertThat(jobs).anyMatch(j -> j.getExtraVars().contains(parameterResolved));
                });
    }
}
