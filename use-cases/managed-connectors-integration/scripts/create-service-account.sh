#!/bin/sh

# rhoas login --print-sso-url

echo "Please enter the service account name: "
read SERVICE_ACCOUNT_NAME

OLD_SERVICE_ACCOUNT_ID=$(rhoas service-account list | grep $SERVICE_ACCOUNT_NAME | awk '{print $1}')

rhoas service-account delete -y --id=$OLD_SERVICE_ACCOUNT_ID

rhoas service-account create --output-file=./service-acct-credentials.json --file-format=json --overwrite --short-description=lucamolteni-managedconnector-serviceaccount

