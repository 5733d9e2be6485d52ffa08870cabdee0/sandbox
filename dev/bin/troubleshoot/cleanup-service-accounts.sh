#!/bin/bash
# To be used in case of service accounts which cannot be created due to limit exceeded
# Example:
#
#   cleanup-service-accounts.sh => will use the USERNAME env var to look for service accounts
#   cleanup-service-accounts.sh $username => look for service accounts with owner matching the given `username`
#
# Created due to https://issues.redhat.com/browse/MGDSTRM-7540
# This requires the ocm command: https://github.com/openshift-online/ocm-cli
# You will to login with `ocm login --token [...]`. Token can be found here: https://console.redhat.com/openshift/token

username=$1

if [ -z "$username" ]; then
    username=${USERNAME}
fi

ids=$(ocm get /api/kafkas_mgmt/v1/service_accounts --parameter size=1000 | jq '.items[] | {id,owner} | join(" ")' | grep ${username} | tr -d '"' | awk '{print $1}')

for id in $ids
do
    ocm delete /api/kafkas_mgmt/v1/service_accounts/${id}
    echo "Deleted service account with id ${id}"
done