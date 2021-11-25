#!/bin/sh

export COS_BASE_PATH=https://cos-fleet-manager-cos.rh-fuse-153f1de160110098c1928a6c05e19444-0000.eu-de.containers.appdomain.cloud
export KAS_BASE_PATH=https://api.openshift.com

export SERVICEACCOUNT_ID=$(cat ./service-acct-credentials.json | jq -r '.clientID')
export SERVICEACCOUNT_SECRET=$(cat ./service-acct-credentials.json | jq -r '.clientSecret')

OCM_TOKEN=$(ocm token)
export OCM_TOKEN