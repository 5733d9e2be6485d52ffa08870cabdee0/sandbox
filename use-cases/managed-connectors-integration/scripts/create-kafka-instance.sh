#!/bin/sh

interval_in_seconds=20

echo "Please enter the managed kafka instance name: "
read KAFKA_INSTANCE_NAME

# Delete
OLD_KAFKA_INSTANCE_ID=$(rhoas kafka list | grep $KAFKA_INSTANCE_NAME | awk '{print $1}')
if test -z "$OLD_KAFKA_INSTANCE_ID"
then
      echo "No Kafka Instance to to delete"
else
      echo "Attemping to delete kafka instance $OLD_KAFKA_INSTANCE_ID"
      rhoas kafka delete -y --id=$OLD_KAFKA_INSTANCE_ID

      printf "\nPolling kafka deletion every $interval_in_seconds seconds, until deleted\n"
      while true;
      do
          rhoas kafka list | grep $KAFKA_INSTANCE_NAME
          status=$?
          printf "\r$(date +%H:%M:%S): $status";
          if [ $status = 1 ]; then
              printf "Kafka Instance deleted\n";
              break;
          fi;
          sleep $interval_in_seconds;
      done
fi


rhoas kafka create --name=$KAFKA_INSTANCE_NAME
# rhoas kafka describe

SERVICEACCOUNT_ID=$(cat ./service-acct-credentials.json | jq -r '.clientID')

printf "\nPolling kafka creation every $interval_in_seconds seconds, until 'ready'\n"
while true;
do
    status=$(rhoas kafka list | grep $KAFKA_INSTANCE_NAME | awk '{print $5}');
    printf "\r$(date +%H:%M:%S): $status";
    if [ "$status" = "ready" ]; then
        printf "\nKafka Instance provisioned\n";
        break;
    fi;
    sleep $interval_in_seconds;
done

rhoas kafka acl grant-access -y --consumer --producer --service-account $SERVICEACCOUNT_ID --topic-prefix slacktopic  --group all

BASE_URL=$(rhoas kafka describe --name=$KAFKA_INSTANCE_NAME | jq -r '.bootstrap_server_host')
export BASE_URL

SERVICEACCOUNT_SECRET=$(cat ./service-acct-credentials.json | jq -r '.clientSecret')

rhoas kafka topic create --name=slacktopic

echo "Kafka Instance created, now set\n\texport KAFKA_BASE_PATH=$BASE_URL\n\texport SERVICEACCOUNT_ID=$SERVICEACCOUNT_ID\n\texport SERVICEACCOUNT_SECRET=$SERVICEACCOUNT_SECRET"
