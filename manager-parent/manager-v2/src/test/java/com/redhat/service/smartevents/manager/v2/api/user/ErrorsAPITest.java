package com.redhat.service.smartevents.manager.v2.api.user;

import java.util.Collection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.models.responses.ErrorListResponse;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorResponse;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;
import com.redhat.service.smartevents.test.exceptions.ExceptionHelper;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ErrorsAPITest {

    private static Collection<Class<?>> exceptionClasses;

    @BeforeAll
    private static void init() {
        exceptionClasses = ExceptionHelper.getUserExceptions();
    }

    @Test
    void testGetList() {
        ErrorListResponse response = given().contentType(ContentType.JSON).when().get(V2APIConstants.V2_ERROR_API_BASE_PATH).as(ErrorListResponse.class);
        assertThat(exceptionClasses).hasSize((int) response.getTotal());
        assertThat(response.getItems().isEmpty()).isFalse();
        for (ErrorResponse item : response.getItems()) {
            assertThat(item).isEqualTo(given().contentType(ContentType.JSON).when().get(item.getHref()).as(ErrorResponse.class));
        }
    }
}
