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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.bridge.infra.models.ListResult;
import com.redhat.service.bridge.infra.models.QueryInfo;

@ApplicationScoped
public class ErrorInMemoryDAO implements ErrorDAO {

    private static final Logger logger = LoggerFactory.getLogger(ErrorInMemoryDAO.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class ErrorInfo {
        private String exception;
        private int id;
        private String reason;

        public String getException() {
            return exception;
        }

        public int getId() {
            return id;
        }

        public String getReason() {
            return reason;
        }
    }

    private Map<Integer, Error> errorsFromId;
    private Map<String, Error> errorsFromExc;
    private List<Error> errorList;

    @PostConstruct
    void init() {
        errorList = new ArrayList<>();
        errorsFromId = new HashMap<>();
        errorsFromExc = new HashMap<>();

        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("exception/exceptionInfo.json")) {
            if (is != null) {
                MAPPER.readValue(is, new TypeReference<List<ErrorInfo>>() {
                }).forEach(this::populate);
            } else {
                logger.error("Cannot find file containing errors in classpath");
            }
        } catch (IOException io) {
            logger.error("Error closing file with exception errors", io);
        }
    }

    private void populate(ErrorInfo errorInfo) {
        Error error = new Error(errorInfo.getId(), "OpenBridge-" + errorInfo.getId(), errorInfo.getReason());
        errorList.add(error);
        errorsFromId.put(error.getId(), error);
        errorsFromExc.put(errorInfo.getException(), error);
    }

    @Override
    public ListResult<Error> findAll(QueryInfo queryInfo) {
        int start = queryInfo.getPageNumber() * queryInfo.getPageSize();
        return new ListResult<>(start >= errorList.size() ? Collections.emptyList() : errorList.subList(start, Math.min(start + queryInfo.getPageSize(), errorList.size())), queryInfo.getPageNumber(),
                errorList.size());
    }

    @Override
    public Error findById(int errorId) {
        return errorsFromId.get(errorId);
    }

    @Override
    public Error findByException(Exception ex) {
        return errorsFromExc.get(ex.getClass().getName());
    }
}
