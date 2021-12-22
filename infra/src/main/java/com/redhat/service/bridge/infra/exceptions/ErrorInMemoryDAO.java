package com.redhat.service.bridge.infra.exceptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;
import com.redhat.service.bridge.infra.utils.Constants;

import io.quarkus.runtime.Quarkus;

@ApplicationScoped
public class ErrorInMemoryDAO implements ErrorDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorInMemoryDAO.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class ErrorInfo {
        @JsonProperty("exception")
        private String exception;

        @JsonProperty("id")
        private int id;

        @JsonProperty("reason")
        private String reason;

        @JsonProperty("isUserException")
        private boolean isUserException;

        public String getException() {
            return exception;
        }

        public int getId() {
            return id;
        }

        public String getReason() {
            return reason;
        }

        public boolean isUserException() {
            return isUserException;
        }

        public Error toError() {
            return new Error(id, Constants.API_IDENTIFIER_PREFIX + id, reason, isUserException);
        }
    }

    private final Map<Integer, Error> userErrorsFromId = new HashMap<>();
    private final Map<String, Error> errorsFromExc = new HashMap<>();
    private final List<Error> userErrorList = new ArrayList<>();

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
        }
    }

    private void populate(ErrorInfo errorInfo) {
        Error error = errorInfo.toError();
        if (errorInfo.isUserException()) {
            userErrorList.add(error);
            userErrorsFromId.put(error.getId(), error);
        }
        errorsFromExc.put(errorInfo.getException(), error);
    }

    @Override
    public ListResult<Error> findAllUserErrors(QueryInfo queryInfo) {
        int start = queryInfo.getPageNumber() * queryInfo.getPageSize();
        return new ListResult<>(start >= userErrorList.size() ? Collections.emptyList() : userErrorList.subList(start, Math.min(start + queryInfo.getPageSize(), userErrorList.size())),
                queryInfo.getPageNumber(),
                userErrorList.size());
    }

    @Override
    public Error findUserErrorById(int errorId) {
        return userErrorsFromId.get(errorId);
    }

    @Override
    public Error findByException(Exception ex) {
        return errorsFromExc.get(ex.getClass().getName());
    }

    @Override
    public Error findByException(Class clazz) {
        return errorsFromExc.get(clazz.getName());
    }
}
