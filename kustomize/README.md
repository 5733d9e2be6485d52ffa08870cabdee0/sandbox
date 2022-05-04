# Kustomization

This directory contains the GitOps project used for the deployment of the platform.

## How do we deploy to `dev` and `stable` environments

We have 2 environments: `dev` and `stable`. The idea is that when the platform is running fine on `dev`, then it is ready to be promoted to `stable`.
This kustomize project is organized as following:
- `base`: this layer should contain the resources that are in common to every platform (k8s and ocp).
- `base-openshift`: this layer should contain the resources that are in common to the ocp environments we have (`dev` and `stable`).
- `overlays/dev`: this overlay should contain only the configurations specific to the `dev` environment (for example the secrets).
- `overlays/stable`: this overlay should contain only the configurations specific to the `stable` environment.
- `overlays/minikube`: this overlay should contain only the configurations specific to the `minikube/k8s` environments.

The kustomize project defines the `what`, but we manage `when` the changes are applied to every specific environment with the `dev` and `stable` branches of this repository. 

The ArgoCD service on the `dev` cluster is watching the branch `dev` of this repository and applies the overlay `dev`.  
On the other side, the ArgoCD service on the `stable` cluster is watching the branch `stable` of this repository and applies the overlay `stable`. 

A `deployer bot` has been implemented to make easy and transparent the deployment to a specific environment. There is only one command available: `/deploy <target_env>`. For example, if you want to deploy to `dev` you will add a comment `/deploy dev` in the pull request that has been merged. If you want to deploy to `stable`, you have to comment with `/deploy stable`.

The workflow for the developer is the following: 

1) The developer wants to modify the services and opens a pull request from his/her fork to the `main` branch of this repository. The kustomize project and all its overlays **must be modified if needed**, according to the changes to the codebase (for example, a new configuration is added). 
2) When the pull request of the developer has been merged
  - if the merged pull request does trigger the build of at least one image -> use the `deployer bot` in the "update kustomization images" pull request
  - if the merged pull request does not trigger any build of the images -> use the `deployer bot` directly in the pull request itself. (this is the case for integration tests and kustomize configuration only PR for example).

## Local Minikube deployment

Requirements:

- Running Minikube instance with enabled Ingress

All supporting services are deployed in dedicated namespaces to provide isolation between components.

In this deployment scenario developer provides operator image, Kustomize deploy all components required by operator and manager into Minikube (apart from Managed Kafka - that needs to be provisioned externally). Can be used to run full e2e testing.

As a prerequisite the developer needs to adjust

- `EVENT_BRIDGE_SSO_URL` in `overlays/minikube/shard/patches/deploy-config.yaml` 
- `INGRESS_OVERRIDE_HOSTNAME` in `overlays/minikube/shard/patches/deploy-config.yaml`  
  It is mainly used by Kind (default hostname is `kind-control-plane`), so you can remove that line if you are using minikube or set the Minikube IP, both will work.
- `overlays/minikube/manager/patches/deploy-config.yaml` to contain proper Minikube IP address. IP address can be retrieved using `minikube ip`. Port value should stay as defined as it references Keycloak Nodeport.
- `overlays/minikube/shard/patches/deploy-config.yaml` to contain the offline token for the webhook robot account. It can be retrieved with the command below
- `overlays/minikube/manager/kustomization.yaml` to contain Managed Kafka bootstrap server URLs and client credentials as well as Managed Connectors bootstrap server URLs and client credentials

```shell
curl --insecure -X POST http://`minikube ip`:30007/auth/realms/event-bridge-fm/protocol/openid-connect/token --user event-bridge:secret -H 'content-type: application/x-www-form-urlencoded' -d 'username=webhook-robot-1&password=therobot&grant_type=password&scope=offline_access' | jq --raw-output '.access_token'
```

Components can be installed using command `kustomize build overlays/minikube | oc apply -f -`.

Note: Some components may not be created fast enough while being required by following resources (for example Strimzi CRD). If you see some error in the command output then rerun the command again.

Manager is available on URL `http://<minikube IP>/manager`, keycloak on `http://<minikube IP>:30007`.

You can run this command to setup `MANAGER_URL` and `KEYCLOAK_URL` variables to your shell:

```bash
.  dev/bin/credentials/local_env
```

Environment can take a significant time to start completely, check status of all components in minikube.

The Minikube startup and provisioning of all required services is scripted in `startMinikubeDeployLocalDev.sh`. The script has following requirements:

- `minikube` binary available on path
- `kustomize` tool installed
- `jq` tool installed
- `kubectl` tool installed

## Demo cluster deployment

### Cluster prerequisites

Required Operators:

- Cluster Monitoring
- Bitnami sealed secrets (via controller deployment below)

## Sealed Secrets

We make use of the BitNami Sealed Secrets Operator to store sensitive information, encrypted in this Git repository.
Things are encrypted using the public key and then decrypted in our demo cluster using the private key.
The private key is only known to the Sealed Secrets Operator in our demo cluster and is managed as part of our
demo cluster setup.

### kubeseal install required once per cluster

https://learnk8s.io/kubernetes-secrets-in-git

### kubeseal create Sealed Secrets using the Bridge Public Key

The public key for encryption needs to be retrieved from the demo cluster using your login.

Log in to the demo cluster and then use the following to retrieve the key from the root of this repository:

```shell
kubeseal --controller-namespace kube-system --fetch-cert > mycert.pem
```

Once you have downloaded the public key certificate, you can use the following commands to encrypt:

```shell

oc create secret generic my-secret --from-literal='DATA=<mydata>' --dry-run=client -o yaml > secret.yaml

kubeseal --cert mycert.pem --scope cluster-wide -o yaml -f secret.yaml > sealedSecret.yaml
```

So for example, in order to create the Grafana secrets, run 

```bash
kubeseal --controller-namespace kube-system --fetch-cert > mycert.pem
BEARER_TOKEN=$(oc serviceaccounts get-token grafana-serviceaccount -n event-bridge-prod)
oc create secret generic grafana-secrets --from-literal='BEARER_TOKEN=$BEARER_TOKEN' --from-literal='GF_SECURITY_ADMIN_USER=<REDACTED>' --from-literal='GF_SECURITY_ADMIN_PASSWORD=<REDACTED>' --dry-run=client -o yaml > secret.yaml
kubeseal --cert mycert.pem --scope cluster-wide -o yaml -f secret.yaml > grafana-secrets.yaml
```

## Deploying Changes to our Demo Cluster

Deployments are managed by ArgoCD (look for the url in the onboarding document).

When a pull request is merged into master, the CD pipeline is triggered and a new docker image is pushed to our quay.io repository. Within 30 minutes, a new pull request should be created to this repository to update the image of the kustomization overlay. When this pull request is merged, ArgoCD detects the changes and applies them to the cluster.

### Manual deployment to a cluster

**Note**: you should not push changes to the demo cluster under the same namespace that is used by the kustomization overlay, because it will conflict with ArgoCD that will detect the difference and will re-deploy the resources that are in the `main` branch of the public repository.

You can of course create the resources under another namespace or another cluster instead.

Before you start the deployment process, ensure that your `oc` CLI is authenticated to our demo cluster.

To do a deployment, use the following from your authenticated `oc` CLI (remember to change the `namespace` in the overlay):

```shell
kustomize build overlays/prod | oc apply -f -
```

The command should complete and exit cleanly.
