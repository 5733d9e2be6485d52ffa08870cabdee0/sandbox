package com.redhat.service.bridge.manager.dao;

import com.redhat.service.bridge.manager.models.Error;
import com.redhat.service.bridge.manager.models.ListResult;
import com.redhat.service.bridge.manager.models.QueryInfo;

public interface ErrorDAO {

    ListResult<Error> findAll(QueryInfo queryInfo);

    Error findById(int errorId);

    Error findByException(Exception ex);
}
