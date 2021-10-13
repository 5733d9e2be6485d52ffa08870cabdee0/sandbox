# Kustomization

This directory contains the GitOps project used for the deployment of the platform. 

### Cluster prereqs
Required Operators:

- Cluster Monitoring
- Bitnami sealed secrets (via controller deployment below)

# Sealed Secrets

We make use of the BitNami Sealed Secrets Operator to store sensitive information, encrypted in this Git repository.
Things are encrypted using the public key and then decrypted in our demo cluster using the private key.
The private key is only known to the Sealed Secrets Operator in our demo cluster and is managed as part of our
demo cluster setup.

### kubeseal install required once per cluster
https://learnk8s.io/kubernetes-secrets-in-git


### kubeseal create Sealed Secrets using the BAaaS Public Key

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

# Deploying Changes to our Demo Cluster

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