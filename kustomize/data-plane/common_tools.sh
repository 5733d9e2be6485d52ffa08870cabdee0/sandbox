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

# Waits until an operator with given subscription name installation finishes (phase=Succeeded), params: operatorSubscriptionName
# Example:
#  wait_until_operator_installed smart-events-shard-operator-subscription
#
function wait_until_operator_installed()
{
  local name=$1

  echo "Waiting for operator CSV"
  OPERATOR_CSV=$(waitForOpResult 30 oc get subscription.operators.coreos.com "$name" -n openshift-operators -o jsonpath={.status.currentCSV})

  echo "Operator CSV found: $OPERATOR_CSV"
  # wait for the resource to be available, oc wait might fail otherwise
  waitForOpResult 60 oc get csv $OPERATOR_CSV -o jsonpath={.status.phase} -n openshift-operators
  # wait for the second time to avoid race condition
  waitForOpResult 30 oc get csv $OPERATOR_CSV -o jsonpath={.status.phase} -n openshift-operators
  echo "Waiting for status.phase=Succeeded"
  # plain oc wait fails on network disconnect, hence the waitForSuccess call
  waitForSuccess 3 oc wait --for=jsonpath='{.status.phase}'=Succeeded --timeout=240s csv $OPERATOR_CSV -n openshift-operators

  status=$?
  if [ $status -ne 0 ]; then
    echo "Operator failed to install, exitcode: $status"
    exit 1
  fi
}
