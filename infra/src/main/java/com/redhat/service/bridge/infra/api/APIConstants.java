package com.redhat.service.bridge.infra.api;

/**
 * Some constants used for URL construction, parameter naming and defaults for the API
 */
public class APIConstants {

    /**
     * Base Path for the user-facing API
     */
    public static final String USER_API_BASE_PATH = "/api/v1/bridges/";

    /**
     * Base Path for Shard facing API.
     */
    public static final String SHARD_API_BASE_PATH = "/api/v1/shard/bridges/";

    /**
     * The page query parameter name
     */
    public static final String PAGE = "page";

    /**
     * The default value for the page query parameter (if it is omitted by the user)
     */
    public static final String PAGE_DEFAULT = "0";

    /**
     * The minimum value of the page query parameter
     */
    public static final int PAGE_MIN = 0;

    /**
     * The size query parameter name
     */
    public static final String PAGE_SIZE = "size";

    /**
     * The minimum value for the size query parameter
     */
    public static final int SIZE_MIN = 1;

    /**
     * The maximum value for the size query parameter
     */
    public static final int SIZE_MAX = 100;

    /**
     * The default value for the size query parameter (if it is omitted by the user)
     */
    public static final String SIZE_DEFAULT = "100";

    private APIConstants() {
    }
}