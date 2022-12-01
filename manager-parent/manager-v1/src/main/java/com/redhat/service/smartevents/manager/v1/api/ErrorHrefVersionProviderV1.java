package com.redhat.service.smartevents.manager.v1.api;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.core.exceptions.AbstractErrorHrefVersionProvider;
import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;

@ApplicationScoped
public class ErrorHrefVersionProviderV1 extends AbstractErrorHrefVersionProvider {

    private static final String PACKAGE_REGEX_MATCH = "com.redhat.service.smartevents.*.v1.*";

    @Override
    protected String getPackageRegexMatch() {
        return PACKAGE_REGEX_MATCH;
    }

    @Override
    protected String getBaseHref() {
        return V1APIConstants.V1_ERROR_API_BASE_PATH;
    }
}
