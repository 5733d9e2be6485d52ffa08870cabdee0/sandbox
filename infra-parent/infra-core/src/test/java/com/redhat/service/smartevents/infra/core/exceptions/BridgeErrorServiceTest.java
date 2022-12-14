package com.redhat.service.smartevents.infra.core.exceptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryPageInfo;
import com.redhat.service.smartevents.test.exceptions.ExceptionHelper;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@QuarkusTest
class BridgeErrorServiceTest {

    @Inject
    BridgeErrorService service;

    private static Collection<Class<?>> userExceptionClasses;
    private static Collection<Class<?>> platformExceptionClasses;

    @BeforeAll
    static void init() {
        userExceptionClasses = ExceptionHelper.getUserExceptions();
        platformExceptionClasses = ExceptionHelper.getPlatformExceptions();
    }

    @Test
    void testUserErrorList() {
        final int pageSize = 2;
        Collection<BridgeError> bridgeErrors = new ArrayList<>();
        ListResult<BridgeError> result;
        int page = 0;
        do {
            result = service.getUserErrors(new QueryPageInfo(page++, pageSize));
            bridgeErrors.addAll(result.getItems());
        } while (result.getSize() == pageSize);
        assertThat(userExceptionClasses)
                .withFailMessage(String.format("Exception classes: %s Errors: %s", userExceptionClasses, bridgeErrors))
                .hasSize(bridgeErrors.size());
        bridgeErrors.forEach(this::checkId);
    }

    @Test
    void testGetError() {
        Collection<Class<?>> allExceptionClasses = new ArrayList<>(userExceptionClasses);
        allExceptionClasses.addAll(platformExceptionClasses);
        for (BridgeError be : getBridgeErrors(allExceptionClasses)) {
            Optional<BridgeError> oById = service.getError(be.getId());
            assertThat(oById).isPresent();
            assertThat(oById).contains(be);
        }
    }

    @Test
    void testGetUserError() {
        for (BridgeError be : getBridgeErrors(userExceptionClasses)) {
            Optional<BridgeError> oById = service.getUserError(be.getId());
            assertThat(oById).isPresent();
            assertThat(oById).contains(be);

            int id = be.getId();
            assertThatThrownBy(() -> service.getPlatformError(id)).isInstanceOf(ItemNotFoundException.class);
        }
    }

    @Test
    void testGetPlatformError() {
        for (BridgeError be : getBridgeErrors(platformExceptionClasses)) {
            Optional<BridgeError> oById = service.getPlatformError(be.getId());
            assertThat(oById).isPresent();
            assertThat(oById).contains(be);

            int id = be.getId();
            assertThatThrownBy(() -> service.getUserError(id)).isInstanceOf(ItemNotFoundException.class);
        }
    }

    @Test
    void testUserErrorException() {
        userExceptionClasses.forEach(this::checkExceptionIsInCatalog);
    }

    @Test
    void testPlatformErrorException() {
        platformExceptionClasses.forEach(this::checkExceptionIsNotInCatalog);
    }

    private void checkId(BridgeError bridgeError) {
        assertThat(service.getUserError(bridgeError.getId())).isPresent();
    }

    private void checkExceptionIsInCatalog(Class<?> clazz) {
        BridgeError bridgeError = service.getError(clazz).get();
        assertThat(bridgeError)
                .withFailMessage(String.format("exception %s not found in the errors", clazz))
                .isNotNull();
        assertThat(service.getUserError(bridgeError.getId()))
                .withFailMessage(String.format("exception %s not found in the user errors", clazz))
                .isNotEmpty();
    }

    private void checkExceptionIsNotInCatalog(Class<?> clazz) {
        BridgeError bridgeError = service.getError(clazz).get();
        assertThat(bridgeError)
                .withFailMessage(String.format("exception %s not found in the errors", clazz))
                .isNotNull();
        assertThat(service.getUserError(bridgeError.getId()))
                .withFailMessage(String.format("exception %s should not be in the user errors", clazz))
                .isEmpty();
    }

    private List<BridgeError> getBridgeErrors(Collection<Class<?>> classes) {
        return classes.stream().map(c -> service.getError(c)).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

}
