package com.redhat.service.smartevents.infra.utils;

//@NoArgsConstructor(access= AccessLevel.PRIVATE)
//@SuppressWarnings("java:S1118")
public class Constants {
    private Constants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String API_IDENTIFIER_PREFIX = "OPENBRIDGE-";
    public static final String HTTP_SCHEME = "http://";
    public static final String HTTPS_SCHEME = "https://";
}
