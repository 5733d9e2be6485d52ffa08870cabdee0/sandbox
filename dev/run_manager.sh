#!/bin/sh

# import utils functions
dev_dir="$(dirname "$0")"
. $dev_dir/utils/utils.sh

function run_manager(){
  local minikube_ip=$1

  mvn clean compile \
    -f $dev_dir/../manager/pom.xml \
    -Dminikubeip=$minikube_ip \
    quarkus:dev
}

function main(){
  MINIKUBE_IP=$(minikube ip)

  echo $(valid_ip $MINIKUBE_IP)

  if valid_ip $MINIKUBE_IP ; then
    echo "Minikube is up and running at "$MINIKUBE_IP
    echo "Starting the manager.."
    run_manager $MINIKUBE_IP
  else
    echo "Can not retrieve minikube ip. Please make sure that minikube is running fine."
  fi
}

main