#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"
source_name=$TODAY_SOURCE_NAME
source_type='slack'

usage() {
    echo 'Usage: create-source.sh [OPTIONS]'
    echo
    echo 'Options:'
    echo '  -n                  Source name. Default is the generated $TODAY_SOURCE_NAME'
    echo '  -t                  Source type. Default is `slack`. Available values: slack, awss3'
    echo
    echo 'Examples:'
    echo '  # Create default slack source'
    echo '  sh create-source.sh'
    echo
    echo '  # Create source_name source of type webhook type'
    echo '  sh create-source.sh -n source_name -t awss3'
}

while getopts "t:n:h" i
do
    case "$i"
    in
        n) source_name="${OPTARG}" ;;
        t) source_type="${OPTARG}" ;;
        h) usage; exit 0 ;;
        :) usage; exit 1 ;; # If expected argument omitted:
        *) usage; exit 1 ;; # If unknown (any other) option
    esac
done
shift "$((OPTIND-1))"

if [ "${source_type}" = 'slack' ]; then
  source_payload='{
   "name": '"\"$source_name\""',
   "source": {
      "type": "slack_source_0.1",
      "parameters": {
         "slack_channel": "mc_source",
         "slack_token": '"\"$SLACK_TOKEN\""'
      }
   }
}'
elif [ "${source_type}" = 'awss3' ]; then
  source_payload='{
   "name": '"\"$source_name\""',
   "source": {
      "type": "aws_s3_source_0.1",
      "parameters": {
            "aws_bucket_name_or_arn": '"\"$S3_BUCKET\""',
            "aws_region": '"\"$S3_REGION\""',
            "aws_access_key" : '"\"$S3_ACCESS_KEY\""',
            "aws_secret_key" : '"\"$S3_SECRET_KEY\""' ,
            "aws_ignore_body" : true ,
            "aws_delete_after_read" : false
      }
   }
}'
else
  echo "Unknown source type: ${source_type}"
  usage
  exit 1
fi

printf "\n\nCreating the ${source_type} source with name $source_name\n"
PROCESSOR_ID=$(curl -s -X POST -H "Authorization: $OB_TOKEN" -H 'Accept: application/json' -H 'Content-Type: application/json' -d "$source_payload" $MANAGER_URL/api/smartevents_mgmt/v1/bridges/$BRIDGE_ID/processors | jq -r .id)

printf "\n\nSource ${source_type} created: $source_name\n"
echo "export PROCESSOR_ID=$PROCESSOR_ID"
