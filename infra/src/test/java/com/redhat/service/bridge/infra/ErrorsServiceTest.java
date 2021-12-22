package com.redhat.service.bridge.infra;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.redhat.service.bridge.infra.exceptions.Error;
import com.redhat.service.bridge.infra.exceptions.ErrorsService;
import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.test.exceptions.ExceptionHelper;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class ErrorsServiceTest {

    @Inject
    ErrorsService service;

    private static Collection<Class<?>> userExceptionClasses;
    private static Collection<Class<?>> platformExceptionClasses;

    @BeforeAll
    private static void init() {
        userExceptionClasses = ExceptionHelper.getUserExceptions();
        platformExceptionClasses = ExceptionHelper.getPlatformExceptions();
    }

    @Test
    void testUserErrorList() {
        final int pageSize = 2;
        Collection<Error> errors = new ArrayList<>();
        ListResult<Error> result;
        int page = 0;
        do {
            result = service.getUserErrors(new QueryInfo(page++, pageSize));
            errors.addAll(result.getItems());
        } while (result.getSize() == pageSize);
        assertThat(userExceptionClasses).hasSize(errors.size()).withFailMessage(String.format("Exception classes: %s Errors: %s", userExceptionClasses, errors));
        errors.forEach(this::checkId);
    }

    @Test
    void testUserErrorException() {
        userExceptionClasses.forEach(this::checkExceptionIsInCatalog);
    }

    @Test
    void testPlatformErrorException() {
        platformExceptionClasses.forEach(this::checkExceptionIsNotInCatalog);
    }

    private void checkId(Error error) {
        assertThat(service.getUserError(error.getId()).isPresent()).isTrue();
    }

    private void checkExceptionIsInCatalog(Class<?> clazz) {
        Error error = service.getError(clazz).get();
        assertThat(error).isNotNull().withFailMessage(String.format("exception %s not found in the errors", clazz));
        assertThat(service.getUserError(error.getId()).isPresent()).isTrue().withFailMessage(String.format("exception %s not found in the user errors", clazz));
    }

    private void checkExceptionIsNotInCatalog(Class<?> clazz) {
        Error error = service.getError(clazz).get();
        assertThat(error).isNotNull().withFailMessage(String.format("exception %s not found in the errors", clazz));
        assertThat(service.getUserError(error.getId()).isPresent()).isFalse().withFailMessage(String.format("exception %s should not be in the user errors", clazz));
    }

}
