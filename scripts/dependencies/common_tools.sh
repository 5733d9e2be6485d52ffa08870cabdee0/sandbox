#!/bin/bash

# Waits for given command to produce a non-empty result and returns the result.
# Parameters: numberOfRetries command
function waitForOpResult() {
    max_retry=$1
    shift
    counter=0
    cmd=$@
    res=$($cmd)
    until [ -n "$res" ]
    do
       sleep 5
       [[ counter -eq $max_retry ]] && echo "Failed! waitForOpResult reached max retry count $max_retry." >&2 && exit 1
       echo "Trying again. Try #$counter" >&2
       res=$($cmd)
       ((counter++))
    done
    echo $res
}

# Waits for given command to finish successfully (exitcode 0).
# Parameters: numberOfRetries command
function waitForSuccess() {
    max_retry=$1
    shift
    counter=0
    until $@
    do
       sleep 5
       [[ counter -eq $max_retry ]] && echo "Failed! Wait for success reached max retry count $max_retry." && exit 1
       echo "Trying again. Try #$counter"
       ((counter++))
    done
}

# Installs an operator according to given subscription resource and waits, params: operatorSubscriptionResource, namespace (defaults to openshift-operators)
# Example:
#  install_operator_and_wait serverless/serverlessSub.yaml
#
function install_operator_and_wait()
{
  local resource=$1
  local namespace=${2:-openshift-operators}

  echo "Applying $resource"
  oc apply -f "$resource"
  sleep 2

  echo "Waiting for operator installation plan"
  OPERATOR_INST_PLAN=$(waitForOpResult 30 oc get -f "$resource" -n $namespace -o jsonpath={.status.installplan.name})
  echo "Operator installation plan found: $OPERATOR_INST_PLAN"

  echo "Approving operator installation plan"
  waitForSuccess 3 oc patch installplan.operators.coreos.com $OPERATOR_INST_PLAN -n $namespace --patch-file approve-ip-patch.json --type json

  echo "Waiting for operator CSV"
  OPERATOR_CSV=$(waitForOpResult 30 oc get -f "$resource" -n $namespace -o jsonpath={.status.currentCSV})

  echo "Operator CSV found: $OPERATOR_CSV"
  # wait for the resource to be available, oc wait might fail otherwise
  waitForOpResult 60 oc get csv $OPERATOR_CSV -o jsonpath={.status.phase} -n $namespace
  # wait for the second time to avoid race condition
  waitForOpResult 30 oc get csv $OPERATOR_CSV -o jsonpath={.status.phase} -n $namespace
  echo "Waiting for status.phase=Succeeded"
  # plain oc wait fails on network disconnect, hence the waitForSuccess call
  waitForSuccess 3 oc wait --for=jsonpath='{.status.phase}'=Succeeded --timeout=240s csv $OPERATOR_CSV -n $namespace

  status=$?
  if [ $status -ne 0 ]; then
    echo "Operator failed to install, exitcode: $status"
    exit 1
  fi
}

# Removes operator CSV and subscription, params: operatorSubscriptionResource, namespace (defaults to openshift-operators)
function uninstall_operator()
{
  local resource=$1
  local namespace=${2:-openshift-operators}

  echo "Deleting CSV and subscription for $resource"
  # Find the operator CSV first
  OPERATOR_CSV=$(waitForOpResult 3 oc get -f "$resource" -n "$namespace" -o jsonpath={.status.installedCSV})
  echo "Found operator CSV for removal: $OPERATOR_CSV"
  waitForSuccess 3 oc delete -f "$resource" -n $namespace --ignore-not-found=true
  waitForSuccess 3 oc delete csv $OPERATOR_CSV -n $namespace --ignore-not-found=true
}