package com.redhat.service.smartevents.infra.core.api;

/**
 * Some constants used for URL construction, parameter naming and defaults for the API
 */
public class APIConstants {

    /**
     * Base Path for the Smart Events application.
     */
    public static final String ROOT = "/api/smartevents_mgmt";

    /**
     * Account id attribute claim key for an account token.
     */
    public static final String ACCOUNT_ID_USER_ATTRIBUTE_CLAIM = "account_id";

    /**
     * Organisation id attribute claim key for a user token.
     */
    public static final String ORG_ID_USER_ATTRIBUTE_CLAIM = "org_id";

    /**
     * Account id attribute claim key for a service account token.
     */
    public static final String ACCOUNT_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM = "rh-user-id";

    /**
     * Organisation id attribute claim key for a service account token.
     */
    public static final String ORG_ID_SERVICE_ACCOUNT_ATTRIBUTE_CLAIM = "rh-org-id";

    /**
     * User id attribute claim key for an owner token.
     * Although the attribute is called 'username' it is consistently referred to as 'owner' in Managed Services.
     */
    public static final String USER_NAME_ATTRIBUTE_CLAIM = "username";

    /**
     * Alternative User id attribute claim key for an owner token.
     * Although the attribute is called 'preferred_username' it is consistently referred to as 'owner' in Managed Services.
     */
    public static final String USER_NAME_ALTERNATIVE_ATTRIBUTE_CLAIM = "preferred_username";

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

    /**
     * The name query filter parameter name
     */
    public static final String FILTER_NAME = "name";

    /**
     * The status query filter parameter name
     */
    public static final String FILTER_STATUS = "status";

    /**
     * The (Processor) type query filter parameter name
     */
    public static final String FILTER_PROCESSOR_TYPE = "type";

    /**
     * Header key for RHOSE's Bridge Id
     */
    public static final String RHOSE_BRIDGE_ID_HEADER = "rhose-bridge-id";
    /**
     * Header key for RHOSE's Processor Id
     */
    public static final String RHOSE_PROCESSOR_ID_HEADER = "rhose-processor-id";
    /**
     * Header key for the original source of an Event processed by RHOSE.
     */
    public static final String RHOSE_ORIGINAL_EVENT_SOURCE_HEADER = "rhose-original-event-source";
    /**
     * Header key for the original ID of an Event processed by RHOSE.
     */
    public static final String RHOSE_ORIGINAL_EVENT_ID_HEADER = "rhose-original-event-id";
    /**
     * Header key for the original type of an Event processed by RHOSE.
     */
    public static final String RHOSE_ORIGINAL_EVENT_TYPE_HEADER = "rhose-original-event-type";
    /**
     * Header key for the original subject of an Event processed by RHOSE.
     */
    public static final String RHOSE_ORIGINAL_EVENT_SUBJECT_HEADER = "rhose-original-event-subject";
    /**
     * Header key for RHOSE's BridgeError code.
     */
    public static final String RHOSE_ERROR_CODE_HEADER = "rhose-error-id";

    private APIConstants() {
    }
}
