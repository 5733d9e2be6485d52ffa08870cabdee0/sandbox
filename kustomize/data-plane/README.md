# Data Plane deployment

The Data Plane deployment script creates a subscription for the shard-operator using a given [index image](environment-properties.env).
In addition, it creates necessary ConfigMap and Secret resources according to given environment variables specified 
in [environment-properties.env](environment-properties.env) and [secret-properties.env](secret-properties.env). 
Inspired by https://mbuffa.github.io/tips/20210720-kustomize-environment-variables/.

## Installation
1. Set required operator parameters:
- The env vars for shard-operator deployment are specified in [environment-properties.env](environment-properties.env), 
for instance (replace with the expected values for your environment):
  ```
    EVENT_BRIDGE_FLEET_SHARD_INDEX_IMAGE
    EVENT_BRIDGE_SSO_URL=http://keycloak:8180/auth/realms/event-bridge-fm
    EVENT_BRIDGE_MANAGER_URL=http://event-bridge:8080
  ```
  Please note that not providing a value in the properties file (such as for `EVENT_BRIDGE_FLEET_SHARD_INDEX_IMAGE`) 
  makes kustomize use a value from the environment variable with the same name.

  **Important:** The values set in the properties file take precedence over environment variables.
- The secrets for shard-operator deployment are specified in [secret-properties.env](secret-properties.env):
  ```
    EVENT_BRIDGE_SHARD_ID
    EVENT_BRIDGE_SSO_CLIENT_ID
    EVENT_BRIDGE_SSO_SECRET
    WEBHOOK_CLIENT_ID
    WEBHOOK_CLIENT_SECRET
    WEBHOOK_TECHNICAL_ACCOUNT_ID
  ```
  No values are specified in the properties file, they are expected to be set in the environment variables.
3. Log in to target cluster (oc login)
4. Run [deployDataPlane.sh](deployDataPlane.sh)

## Uninstallation
1. Log in to target cluster (oc login)
2. Run [undeployDataPlane.sh](undeployDataPlane.sh)

