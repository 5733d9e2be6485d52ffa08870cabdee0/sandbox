package com.redhat.service.smartevents.infra.exceptions;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.redhat.service.smartevents.infra.api.APIConstants;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.AlreadyExistingItemException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.ListResult;
import com.redhat.service.smartevents.infra.models.queries.QueryPageInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class BridgeErrorInMemoryDAOTest {

    private static BridgeErrorInMemoryDAO dao;

    @BeforeAll
    public static void setup() {
        dao = new BridgeErrorInMemoryDAO();
        dao.init();
    }

    @ParameterizedTest
    @EnumSource(BridgeErrorType.class)
    void testFindAllErrorsByType(BridgeErrorType type) {
        ListResult<BridgeError> errors = dao.findAllErrorsByType(new QueryPageInfo(APIConstants.PAGE_MIN, APIConstants.SIZE_MAX), type);
        assertThat(errors).isNotNull();
        assertThat(errors.getItems()).isNotEmpty();
    }

    @Test
    void testFindErrorById() {
        BridgeError error = dao.findErrorById(1);
        assertThat(error).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("findErrorByIdAndType")
    void testFindErrorByIdAndType(int id, BridgeErrorType type, boolean found) {
        if (found) {
            BridgeError error = dao.findErrorByIdAndType(id, type);
            assertThat(error).isNotNull();
        } else {
            assertThatThrownBy(() -> dao.findErrorByIdAndType(id, type)).isInstanceOf(ItemNotFoundException.class);
        }
    }

    @Test
    void testFindByExceptionClass() {
        BridgeError error = dao.findByException(AlreadyExistingItemException.class);
        assertThat(error).isNotNull();
        assertThat(error.getId()).isEqualTo(1);

        assertThat(dao.findByException(NullPointerException.class)).isNull();
    }

    @Test
    void testFindByExceptionInstance() {
        BridgeError error = dao.findByException(new AlreadyExistingItemException("Duplicate"));
        assertThat(error).isNotNull();
        assertThat(error.getId()).isEqualTo(1);

        assertThat(dao.findByException(new NullPointerException())).isNull();
    }

    private static Stream<Arguments> findErrorByIdAndType() {
        return Stream.of(
                Arguments.of(1, BridgeErrorType.USER, true),
                Arguments.of(1, BridgeErrorType.PLATFORM, false),
                Arguments.of(1000, BridgeErrorType.USER, false),
                Arguments.of(11, BridgeErrorType.PLATFORM, true),
                Arguments.of(11, BridgeErrorType.USER, false),
                Arguments.of(1111, BridgeErrorType.PLATFORM, false));
    }
}
