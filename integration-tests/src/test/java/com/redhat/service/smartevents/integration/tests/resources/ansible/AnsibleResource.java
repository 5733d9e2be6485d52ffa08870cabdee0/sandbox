package com.redhat.service.smartevents.integration.tests.resources.ansible;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static io.restassured.RestAssured.given;

public class AnsibleResource {

    private static final String ANSIBLE_ENDPOINT = System.getProperty("ansible.endpoint") + "/api/v2";
    private static final String ANSIBLE_USERNAME = System.getProperty("ansible.username");
    private static final String ANSIBLE_PASSWORD = System.getProperty("ansible.password");

    public static List<String> jobTemplatesList(String name) {
        final List<String> templateNames = given()
                .baseUri(ANSIBLE_ENDPOINT)
                .auth().preemptive().basic(ANSIBLE_USERNAME, ANSIBLE_PASSWORD)
                .relaxedHTTPSValidation()
                .basePath("job_templates")
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getJsonObject("results.name");

        return templateNames;
    }

    public static int jobTemplateID(String name) {
        final int templateID = given()
                .baseUri(ANSIBLE_ENDPOINT)
                .auth().preemptive().basic(ANSIBLE_USERNAME, ANSIBLE_PASSWORD)
                .relaxedHTTPSValidation()
                .basePath("job_templates")
                .queryParam("name", name)
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getInt("results.id[0]");

        return templateID;
    }

    public static List<Job> jobs(int jobTemplateId, int days, int maxItems) {
        final LocalDate now = LocalDate.now();
        final LocalDate startTime = now.minusDays(days);

        final List<Job> jobsData = given()
                .baseUri(ANSIBLE_ENDPOINT)
                .auth().preemptive().basic(ANSIBLE_USERNAME, ANSIBLE_PASSWORD)
                .relaxedHTTPSValidation()
                .basePath("jobs")
                .queryParam("job_template_id", jobTemplateId)
                .queryParam("created__gt", startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .queryParam("page_size", maxItems)
                .when()
                .get()
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("results", Job.class);

        return jobsData;
    }
}
