# Managed Connectors

This document contains notes on how we configured Managed Connectors to deploy connectors directly inside our own OpenShift clusters.

**IMPORTANT #1: this is not required for local development with minikube.**

**IMPORTANT #2: this configurations considers only camel-based connectors, whose catalog can be found [here](https://github.com/bf2fc6cc711aee1a0c2a/cos-fleet-catalog-camel/tree/main/cos-fleet-catalog-connectors).**

## Prelimiary operations

### Required CLI tools

* [kubectl](https://kubernetes.io/docs/reference/kubectl/) must be installed and configured to access the cluster where
Managed Connectors is supposed to be installed.
* [ocm](https://github.com/openshift-online/ocm-cli) must be installed.

### Managed Connectors repositories

The content of [cos-fleetshard](https://github.com/bf2fc6cc711aee1a0c2a/cos-fleetshard) and [cos-tools](https://github.com/bf2fc6cc711aee1a0c2a/cos-tools)
repositories is required.

The following commands assume the two repos are checked out inside the current directory at the branch `main` (the default one), like this:

```text
.
├── cos-fleetshard
└── cos-tools
```

### Environment variables

Export the following environment variables:

```shell
export COS_BASE_PATH=https://cos-fleet-manager-cos.rh-fuse-153f1de160110098c1928a6c05e19444-0000.eu-de.containers.appdomain.cloud
export KAS_BASE_PATH=https://api.openshift.com
```

### Offline token

Obtain your offline token from https://console.redhat.com/openshift/token.

##  Configuration

### Step 1: Namespace

**IMPORTANT:** everything will be installed in the `cos` namespace. If you want to use a different one, make sure you change it accordingly in all the following commands.

Create the namespace with:

```shell
kubectl create ns cos
```

### Step 2: Pull secret

A secret named `cos-pull-secret` must be configured in the namespace in order for the cluster to successfully pull the docker images of the connectors.

Create a file YAML named `cos-pull-secret.yaml` with the following content:

```yaml
apiVersion: v1
kind: Secret
type: kubernetes.io/dockerconfigjson
metadata:
  name: cos-pull-secret
data:
  .dockerconfigjson: <base64_encoded_secret>
```

**IMPORTANT:** ask the Managed Connector team or the SmartEvents devs for the content of `.dockerconfigjson`.

### Step 3: Camel K operator

#### Via OperatorHub (preferred way)

1. In a browser, open the OpenShift console of your destination cluster.
2. Navigate to `Operators -> OperatorHub` in the left menu.
3. Search for `Camel K Operator`.
4. Follow the installation wizard. **Make sure you install it for "All Namespaces".**

#### Manually (make sure you know what you're doing)

1. Download and install the [Camel K CLI](https://github.com/apache/camel-k/releases/) (`kamel`).
2. Run the following command:

```shell
kamel install --olm=false --skip-registry-setup
```

### Step 4: Resources

1. Install CRDs with:

```shell
./cos-fleetshard/etc/scripts/deploy_fleetshard_crds.sh
```

2. Install camel operator and sync with:

```shell
kubectl -n cos apply -k etc/kubernetes/operator-camel/local
kubectl -n cos apply -k etc/kubernetes/sync/local
```

3. Check that the deployments are created successfully and are ready with:

```shell
$ kubectl -n cos get deployments
NAME                            READY   UP-TO-DATE   AVAILABLE   AGE
cos-fleetshard-operator-camel   1/1     1            1           3d
cos-fleetshard-sync             1/1     1            1           3d
```

### Step 5: Tools

1. Set `cos` as the default namespace with:

```shell
kubectl config set-context --current --namespace=cos
```

2. Login with `ocm` using your offline token:

```shell
ocm login --token <offline_token>
```

3. Create Managed Connector cluster instance, where connectors will be deployed (choose the name you prefer), and **store the cluster ID**:

```shell
./cos-tools/bin/create-cluster <cluster_name>
```

4. Check the cluster is created and store the ID if you didn't already do it with:

```shell
./cos-tools/bin/get-clusters
```

5. Create cluster secret with:

```shell
./cos-tools/bin/create-cluster-secret <cluster_id>
```

### Step 6: Update Job

There's no automatic CI/CD mechanism in Managed Connectors to update the operators with the newest version.

The only way at the moment is to configure a Kubernetes [CronJob](https://v1-21.docs.kubernetes.io/docs/concepts/workloads/controllers/cron-jobs/)
that deletes the existing operator pods, so that the deployment triggers the pull of the newest image (if existing) before recreating it.

To configure this job, create a file named `updatejob.yaml` with the following content:

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: cos-updater
  namespace: cos
---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: pod-deleter
  namespace: cos
rules:
  - apiGroups:
      - ''
    resources:
      - pods
    verbs:
      - get
      - watch
      - list
      - delete
---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: cos-updater-pod-deleter
  namespace: cos
subjects:
  - kind: ServiceAccount
    name: cos-updater
    namespace: cos
roleRef:
  kind: Role
  name: pod-deleter
  apiGroup: rbac.authorization.k8s.io
---
apiVersion: batch/v1
kind: CronJob
metadata:
  name: cos-update-by-delete
  namespace: cos
spec:
  concurrencyPolicy: Forbid
  schedule: "@daily"
  successfulJobsHistoryLimit: 1
  failedJobsHistoryLimit: 1
  jobTemplate:
    spec:
      backoffLimit: 2
      activeDeadlineSeconds: 60
      template:
        spec:
          serviceAccountName: cos-updater
          restartPolicy: Never
          containers:
            - name: kubectl
              image: bitnami/kubectl:1.21.6
              command:
              - kubectl
              - delete
              - pods
              - --namespace=cos
              - --selector=app.kubernetes.io/part-of=cos
```

And apply it to the cluster with:

```shell
kubectl -n cos apply -f updatejob.yaml
```

**IMPORTANT:** this file assumes Kubernetes v1.21.6. In case of a different version, update the `image: bitnami/kubectl:1.21.6` line.

### Conclusion

Now you should be ready to deploy Managed Connectors into this namespace using the **obtained Cluster ID and the offline token**.

You can see see the new pods in the `cos` namespace (one for every connector deployed) with:

```shell
kubectl -n cos get pods
```

**IMPORTANT:** the connector pods name start with `mctr-`.

### Extra: Connector logging

It's possible to manually configure enabled a more verbose logging in a Managed Connector with the following `curl` call:

```shell
curl -vvv --insecure --oauth2-bearer "${OFFLINE_TOKEN}" -S -s -D /dev/stderr \
  "${COS_BASE_PATH}/api/connector_mgmt/v1/kafka_connectors/${CONNECTOR_ID}" \
  -XPATCH \
  -H "Content-Type: application/merge-patch+json" \
  -d '{"connector":{"processors":[{"log":{"multiLine":true,"showHeaders":true}}]}}'
```

where `CONNECTOR_ID` is the ID of the connector, `OFFLINE_TOKEN` is the OpenShift offline token and `COS_BASE_PATH` is the variable exported above.

The connector pod will be redeployed and the logs can be checked with:

```shell
kubectl -n cos logs <pod_name>
```
