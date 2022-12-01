package com.redhat.service.smartevents.infra.core.exceptions.mappers;

import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.validation.ConstraintViolation;

import com.redhat.service.smartevents.infra.core.exceptions.AbstractErrorHrefVersionProvider;
import com.redhat.service.smartevents.infra.core.exceptions.ErrorHrefVersionProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestMappersUtils {

    public static final String DEFAULT_HREF_BASE = "/api/v0/errors/";

    public static Instance<ErrorHrefVersionProvider> getDefaultBuildersMock() {
        Instance<ErrorHrefVersionProvider> builders = mock(Instance.class);
        List<ErrorHrefVersionProvider> builderList = Collections.singletonList(new AbstractErrorHrefVersionProvider() {
            @Override
            public boolean accepts(Throwable e) {
                return true;
            }

            @Override
            public boolean accepts(ConstraintViolation<?> cv) {
                return true;
            }

            @Override
            protected String getPackageRegexMatch() {
                // Bypass
                return null;
            }

            @Override
            protected String getBaseHref() {
                return DEFAULT_HREF_BASE;
            }
        });
        when(builders.stream()).then(i -> builderList.stream());
        return builders;
    }

    public static String getDefaultHref(String id) {
        return DEFAULT_HREF_BASE + id;
    }
}
