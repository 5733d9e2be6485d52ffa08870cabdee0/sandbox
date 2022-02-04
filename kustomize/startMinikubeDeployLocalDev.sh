echo "Starting Minikube"
minikube start
minikube addons enable ingress
minikube addons enable ingress-dns

echo "Applying IP replacements"
sed -i -E "s|(.*http://).*(:30007.*)|\1$(minikube ip)\2|" overlays/minikube/shard/patches/deploy-config.yaml
sed -i -E "s|(.*http://).*(:30007.*)|\1$(minikube ip)\2|" overlays/minikube/manager/patches/deploy-config.yaml
sleep 10s

echo "Deploying all resources"
kustomize build overlays/minikube | oc apply -f -

status=$?
if [ $status -ne 0 ]; then
  echo "Some resources fail to deploy (concurrency issues), redeploying"
  sleep 5s
  kustomize build overlays/minikube | oc apply -f -
fi

echo "Wait for Keycloak to start"
MINIKUBE_IP=$(minikube ip)
kubectl wait --for=condition=available --timeout=300s deployment/keycloak -n keycloak
timeout 120 bash -c 'while [[ "$(curl --insecure -s -o /dev/null -w ''%{http_code}'' http://'$MINIKUBE_IP':30007/auth)" != "303" ]]; do sleep 5; done'

echo "Configure shard operator technical bearer token"
TOKEN=$(curl --insecure -X POST http://$MINIKUBE_IP:30007/auth/realms/event-bridge-fm/protocol/openid-connect/token --user event-bridge:secret -H 'content-type: application/x-www-form-urlencoded' -d 'username=webhook-robot-1&password=therobot&grant_type=password&scope=offline_access' | jq --raw-output '.access_token')
sed -i -E "s|(.*WEBHOOK_TECHNICAL_BEARER_TOKEN=).*|\1$TOKEN|" overlays/minikube/shard/kustomization.yaml

echo "Redeploy resources to apply token"
kustomize build overlays/minikube | oc apply -f -
kubectl delete pod --selector=app=event-bridge-shard-operator -n event-bridge-operator

echo "Wait for manager and operator to start"
kubectl wait --for=condition=available --timeout=240s deployment/event-bridge-shard-operator -n event-bridge-operator
kubectl wait --for=condition=available --timeout=240s deployment/event-bridge -n event-bridge-manager
