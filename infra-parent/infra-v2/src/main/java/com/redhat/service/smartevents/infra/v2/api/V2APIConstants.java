package com.redhat.service.smartevents.infra.v2.api;

import com.redhat.service.smartevents.infra.core.api.APIConstants;

/**
 * Some constants used for URL construction for the API
 */
public class V2APIConstants {

    /**
     * Base Path for the Smart Events application API v2.
     */
    public static final String V2_ROOT = APIConstants.ROOT + "/v2";

    /**
     * Base Path for the Cloud Providers API
     */
    public static final String V2_CLOUD_PROVIDERS_BASE_PATH = V2_ROOT + "/cloud_providers";

    /**
     * Base Path for the user-facing API
     */
    public static final String V1_USER_API_BASE_PATH = V2_ROOT + "/bridges/";

    /**
     * Base Path for Shard facing API.
     */
    public static final String V1_SHARD_API_BASE_PATH = V2_ROOT + "/shard/bridges/";

    private V2APIConstants() {
    }
}
