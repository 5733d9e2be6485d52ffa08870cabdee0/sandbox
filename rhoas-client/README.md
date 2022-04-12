# RHOAS client

This module contains a set of beans that allows to interact with the RHOAS services in a reactive async way.
Currently the only service we handle is Managed Kafka.

## Injectable beans

Here is the list of the implemented beans:

|Class|Description|
|-----|-----------|
|[KafkasMgmtV1Client](src/main/java/com/redhat/service/smartevents/rhoas/KafkasMgmtV1Client.java)|Wraps `kafka-management-sdk` sync API calls in reactive async way.|
|[KafkaInstanceAdminClient](src/main/java/com/redhat/service/smartevents/rhoas/KafkaInstanceAdminClient.java)|Wraps `kafka-instance-sdk` sync API calls in reactive async way.|
|[RhoasClient](src/main/java/com/redhat/service/smartevents/rhoas/RhoasClient.java)|Implements complex coarse-grained operations combining the base clients' calls. Fully reactive async.|

## Configuration

These are the properties/env variables to be used to configure this module:

|Property / Env Variable|Description|
|-----------------------|-----------|
|**Property:** `event-bridge.rhoas.mgmt-api.host`</br>**Env var:** `EVENT_BRIDGE_RHOAS_MGMT_API_HOST`|Management API base host. For production use `https://api.openshift.com`.|
|**Property:** `event-bridge.rhoas.instance-api.host`</br>**Env var:** `EVENT_BRIDGE_RHOAS_INSTANCE_API_HOST`|Instance API base host. Obtained prefixing `https://admin-server-` to the instance bootstrap host.|
|**Property:** `event-bridge.rhoas.sso.red-hat.auth-server-url`</br>**Env var:** `EVENT_BRIDGE_RHOAS_SSO_RED_HAT_AUTH_SERVER_URL`|Red Hat SSO authentication server URL|
|**Property:** `event-bridge.rhoas.sso.red-hat.refresh-token`</br>**Env var:** `EVENT_BRIDGE_RHOAS_SSO_RED_HAT_REFRESH_TOKEN`|Red Hat SSO refresh token (in this case, the offline token)|
|**Property:** `event-bridge.rhoas.sso.mas.auth-server-url`</br>**Env var:** `EVENT_BRIDGE_RHOAS_SSO_MAS_AUTH_SERVER_URL`|MAS SSO authentication server URL|
|**Property:** `event-bridge.rhoas.sso.mas.client-id`</br>**Env var:** `EVENT_BRIDGE_RHOAS_SSO_MAS_CLIENT_ID`|MAS SSO authentication client ID|
|**Property:** `event-bridge.rhoas.sso.mas.client-secret`</br>**Env var:** `EVENT_BRIDGE_RHOAS_SSO_MAS_CLIENT_SECRET`|MAS SSO authentication client secret|
