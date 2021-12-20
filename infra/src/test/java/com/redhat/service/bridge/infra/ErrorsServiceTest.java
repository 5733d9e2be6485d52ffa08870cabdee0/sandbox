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

    private static Collection<Class<?>> exceptionClasses;

    @BeforeAll
    private static void init() {
        exceptionClasses = ExceptionHelper.getExceptions();
    }

    @Test
    void testErrorList() {
        final int pageSize = 2;
        Collection<Error> errors = new ArrayList<>();
        ListResult<Error> result;
        int page = 0;
        do {
            result = service.getUserErrors(new QueryInfo(page++, pageSize));
            errors.addAll(result.getItems());
        } while (result.getSize() == pageSize);
        assertThat(exceptionClasses).hasSize(errors.size()).withFailMessage(String.format("Exception classes: %s Errors: %s", exceptionClasses, errors));
        errors.forEach(this::checkId);
    }

    @Test
    void testErrorException() {
        exceptionClasses.forEach(this::checkException);
    }

    private void checkId(Error error) {
        assertThat(service.getUserError(error.getId()).isPresent()).isTrue();
    }

    private void checkException(Class<?> clazz) {
        try {
            assertThat(service.getError(clazz.asSubclass(Exception.class).getConstructor(String.class).newInstance("Dummy error message")).isPresent()).isTrue()
                    .withFailMessage(String.format("exception %s not found", clazz));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
