package com.redhat.service.smartevents.infra.v1.api;

import com.redhat.service.smartevents.infra.core.api.APIConstants;

/**
 * Some constants used for URL construction for the API
 */
public class V1APIConstants {

    /**
     * Base Path for the Smart Events application API v1.
     */
    public static final String V1_ROOT = APIConstants.ROOT + "/v1";

    /**
     * Base Path for the Cloud Providers API
     */
    public static final String V1_CLOUD_PROVIDERS_BASE_PATH = V1_ROOT + "/cloud_providers";

    /**
     * Base Path for the user-facing API
     */
    public static final String V1_USER_API_BASE_PATH = V1_ROOT + "/bridges/";

    /**
     * Base Path for the error API
     */
    public static final String V1_ERROR_API_BASE_PATH = V1_ROOT + "/errors/";

    /**
     * Base Path for the source processors schema API
     */
    public static final String V1_SOURCES_SCHEMA_API_BASE_PATH = V1_ROOT + "/schemas/sources/";

    /**
     * Base Path for the actions processors API
     */
    public static final String V1_ACTIONS_SCHEMA_API_BASE_PATH = V1_ROOT + "/schemas/actions/";

    /**
     * Base Path for the schema API
     */
    public static final String V1_SCHEMA_API_BASE_PATH = V1_ROOT + "/schemas/";

    /**
     * Base Path for Shard facing API.
     */
    public static final String V1_SHARD_API_BASE_PATH = V1_ROOT + "/shard/bridges/";

    private V1APIConstants() {
    }
}