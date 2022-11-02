package com.redhat.service.smartevents.infra.core.exceptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.core.models.ListResult;
import com.redhat.service.smartevents.infra.core.models.queries.QueryPageInfo;
import com.redhat.service.smartevents.infra.core.utils.Constants;

import io.quarkus.runtime.Quarkus;

@ApplicationScoped
public class BridgeErrorInMemoryDAO implements BridgeErrorDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(BridgeErrorInMemoryDAO.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class ErrorInfo {
        @JsonProperty("exception")
        private String exception;

        @JsonProperty("id")
        private int id;

        @JsonProperty("reason")
        private String reason;

        @JsonProperty("type")
        private BridgeErrorType type;

        public String getException() {
            return exception;
        }

        public int getId() {
            return id;
        }

        public String getReason() {
            return reason;
        }

        public BridgeErrorType getType() {
            return type;
        }

        public BridgeError toError() {
            return new BridgeError(id, Constants.API_IDENTIFIER_PREFIX + id, reason, type);
        }
    }

    private final Map<Integer, BridgeError> bridgeErrorsFromId = new HashMap<>();
    private final Map<String, BridgeError> errorsFromExc = new HashMap<>();
    private final List<BridgeError> bridgeErrorList = new ArrayList<>();

    @PostConstruct
    void init() {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("exception/exceptionInfo.json")) {
            if (is != null) {
                MAPPER.readValue(is, new TypeReference<List<ErrorInfo>>() {
                }).forEach(this::populate);
            } else {
                LOGGER.error("Cannot find file containing errors in classpath");
                Quarkus.asyncExit(1);
            }
        } catch (IOException io) {
            LOGGER.error("Error closing file with exception errors", io);
            Quarkus.asyncExit(1);
        }
    }

    private void populate(ErrorInfo errorInfo) {
        BridgeError bridgeError = errorInfo.toError();
        bridgeErrorList.add(bridgeError);
        bridgeErrorsFromId.put(bridgeError.getId(), bridgeError);
        errorsFromExc.put(errorInfo.getException(), bridgeError);
    }

    @Override
    public ListResult<BridgeError> findAllErrorsByType(QueryPageInfo queryInfo, BridgeErrorType type) {
        int start = queryInfo.getPageNumber() * queryInfo.getPageSize();
        List<BridgeError> typedErrors = bridgeErrorList.stream()
                .filter(x -> x.getType().equals(type))
                .collect(Collectors.toList());
        return new ListResult<>(
                start >= typedErrors.size() ? Collections.emptyList() : typedErrors.subList(start, Math.min(start + queryInfo.getPageSize(), typedErrors.size())),
                queryInfo.getPageNumber(),
                typedErrors.size());
    }

    @Override
    public BridgeError findErrorById(int errorId) {
        return bridgeErrorsFromId.get(errorId);
    }

    @Override
    public BridgeError findErrorByIdAndType(int errorId, BridgeErrorType type) {
        BridgeError error = bridgeErrorsFromId.get(errorId);
        if (Objects.isNull(error) || !error.getType().equals(type)) {
            throw new ItemNotFoundException(String.format("Error with id %s and type %s not found in the catalog", errorId, type));
        }
        return error;
    }

    @Override
    public BridgeError findByException(Exception ex) {
        return errorsFromExc.get(ex.getClass().getName());
    }

    @Override
    public BridgeError findByException(Class clazz) {
        return errorsFromExc.get(clazz.getName());
    }
}
