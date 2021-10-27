# Shard Operator

Project scaffold for what about to be the Shard Operator.

Useful resources:

1. [Quarkus Operator SDK Sample Project](https://github.com/quarkiverse/quarkus-operator-sdk/tree/2.0.0.CR2/samples)
2. [Quarkus Kubernetes Extension](https://quarkus.io/guides/deploying-to-kubernetes) (used internally by the Operator SDK, all the deployment use cases apply)

See the main EPIC to understand the following up tasks: https://issues.redhat.com/browse/MGDOBR-78

README to be updated once we start implementing all the features.

## Building and Trying in a Local Minikube

````shell
## Make sure that you're pointing to the internal minikube registry
eval $(minikube -p minikube docker-env)
## generate the resources (if namespace not provided, it uses the default - not recommended)
mvn clean install -Pminikube -Dnamespace=mynamespace
## apply the CRD
kubectl apply -f target/kubernetes/bridgeingresses.com.redhat.service.bridge-v1.yml
## install the operator (it's wise to install in a separated ns, so you can just delete it after your tests)
kubectl apply -f target/kubernetes/minikube.yml -n mynamespace
## now you can build and let the plugin deploy the objects for you
mvn install -DskipTests -Pminikube -Dnamespace=mynamespace -Dquarkus.kubernetes.deploy=true  #-Dquarkus.kubernetes.node-port=90909 <-- us this if clashes
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
