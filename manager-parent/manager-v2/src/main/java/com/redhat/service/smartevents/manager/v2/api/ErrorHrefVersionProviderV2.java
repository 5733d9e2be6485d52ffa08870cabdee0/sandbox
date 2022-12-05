package com.redhat.service.smartevents.manager.v2.api;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.core.exceptions.AbstractErrorHrefVersionProvider;
import com.redhat.service.smartevents.infra.v2.api.V2APIConstants;

@ApplicationScoped
public class ErrorHrefVersionProviderV2 extends AbstractErrorHrefVersionProvider {

    private static final String PACKAGE_REGEX_MATCH = "com.redhat.service.smartevents.*.v2.*";

    @Override
    protected String getPackageRegexMatch() {
        return PACKAGE_REGEX_MATCH;
    }

    @Override
    protected String getBaseHref() {
        return V2APIConstants.V2_ERROR_API_BASE_PATH;
    }
}
