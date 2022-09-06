# Shard Operator 

Project scaffold for what about to be the Shard Operator.

Useful resources:

1. [Quarkus Operator SDK Sample Project](https://github.com/quarkiverse/quarkus-operator-sdk/tree/2.0.0.CR2/samples)
2. [Quarkus Kubernetes Extension](https://quarkus.io/guides/deploying-to-kubernetes) (used internally by the Operator SDK, all the deployment use cases apply)

See the main EPIC to understand the following up tasks: https://issues.redhat.com/browse/MGDOBR-78

README to be updated once we start implementing all the features.

## Building and Trying in an OCP cluster

By default, the application is compiled for OCP. Once you are logged in your OCP cluster, you can deploy the operator with the following commands

```shell
## generate the resources (if namespace not provided, it uses the default - not recommended)
mvn clean install -Dnamespace=mynamespace
## Change the docker image name and tag according to your docker remote hub (remember to make the repository public)
docker tag openbridge/shard-operator:latest quay.io/<username>/shard-operator:latest 
docker push quay.io/<username>/shard-operator:latest 
# Operator requires Keycloak instance to be available, install it
oc create secret generic keycloak-realm --from-file=event-bridge-fm-realm-sample.json=../dev/docker-compose/keycloak-config/event-bridge-fm-realm-sample.json -n mynamespace
oc create secret generic keycloak-secrets --from-literal=KEYCLOAK_USER=admin --from-literal=KEYCLOAK_PASSWORD=123 -n mynamespace
oc create -f ../kustomize/base/manager/resources/keycloak.yaml -n mynamespace
oc wait --for=condition=available --timeout=120s deployment/keycloak -n mynamespace
## apply the CRD
oc apply -f target/kubernetes/bridgeingresses.com.redhat.service.bridge-v1.yml
oc apply -f target/kubernetes/bridgeexecutors.com.redhat.service.bridge-v1.yml
## Update the operator image in the yml file
sed -i -e 's/openbridge\/shard-operator:latest/quay.io\/<username>\/shard-operator:latest/g' target/kubernetes/openshift.yml
## install the operator (it's wise to install in a separated ns, so you can just delete it after your tests)
oc apply -f target/kubernetes/openshift.yml -n mynamespace
oc set env deploymentconfig/shard-operator EVENT_BRIDGE_SSO_URL=http://keycloak:8180/auth/realms/event-bridge-fm -n mynamespace
## install the sample
oc apply -f src/main/kubernetes/sample.yml -n mynamespace
```

## Building and Trying in a Local Minikube

````shell
## The minikube ingress and ingress-dns addon must be enabled
minikube addons enable ingress
minikube addons enable ingress-dns
## Make sure that you're pointing to the internal minikube registry
eval $(minikube -p minikube docker-env)
# Operator requires Keycloak instance to be available, install it
kubectl create secret generic keycloak-realm --from-file=event-bridge-fm-realm-sample.json=../dev/docker-compose/keycloak-config/event-bridge-fm-realm-sample.json -n mynamespace
kubectl create secret generic keycloak-secrets --from-literal=KEYCLOAK_USER=admin --from-literal=KEYCLOAK_PASSWORD=123 -n mynamespace
kubectl create -f ../kustomize/base/manager/resources/keycloak.yaml -n mynamespace
kubectl wait --for=condition=available --timeout=120s deployment/keycloak -n mynamespace
## generate the resources (if namespace not provided, it uses the default - not recommended)
mvn clean install -Dquarkus.container-image.build=true -Dnamespace=mynamespace
## apply the CRD
kubectl apply -f target/kubernetes/bridgeingresses.com.redhat.service.bridge-v1.yml
kubectl apply -f target/kubernetes/bridgeexecutors.com.redhat.service.bridge-v1.yml
## install the operator (it's wise to install in a separated ns, so you can just delete it after your tests)
kubectl apply -f target/kubernetes/kubernetes.yml -n mynamespace
kubectl set env deployment/shard-operator EVENT_BRIDGE_SSO_URL=http://keycloak:8180/auth/realms/event-bridge-fm -n mynamespace
## install the sample
kubectl apply -f src/main/kubernetes/sample.yml -n mynamespace
````

You can check if the controller is working correctly by describing the resource with:

```shell
kubectl describe bi
```

Take a look at the status:

```
Name:         my-bridge
Namespace:    default
Labels:       <none>
Annotations:  <none>
API Version:  com.redhat.service.bridge/v1alpha1
Kind:         BridgeIngress
Metadata:
  Creation Timestamp:  2021-10-22T13:18:42Z
  Finalizers:
    bridgeingresses.com.redhat.service.bridge/finalizer
  Generation:  1
Spec:
  Image:  my-image:latest
Status:
  Status:  OK
Events:    <none>
```

