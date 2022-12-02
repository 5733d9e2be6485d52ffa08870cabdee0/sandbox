#!/bin/sh

usage() {
    echo "Usage: $0 [OPTIONS]"
    echo
    echo 'Creates a V2 processor containing Camel K flows'
    echo
    echo 'Options:'
    echo '  -h    Print this help'
    echo '  -b    Destination bridge ID (must already exist)'
    echo '  -n    Name of the processor to be created'
    echo '  -f    YAML file containing the flows defined as Camel DSL'
    echo
    echo 'Environment variables:'
    echo '  MANAGER_URL    API base URL (default = http://localhost:8080)'
    echo '  OB_TOKEN       Authentication token'
}

usage_and_exit() {
  usage; exit $1;
}

for tool in curl yq; do
  which "$tool" &> /dev/null
  if [ $? -ne 0 ]; then
    echo "Required tool \"$tool\" is missing"
    exit 1
  fi
done

while getopts "b:f:n:h" i
do
    case "$i"
    in
        h) usage_and_exit 0 ;;
        b) bridge_id="${OPTARG}" ;;
        f) flow_file="${OPTARG}" ;;
        n) processor_name="${OPTARG}" ;;
        :) usage_and_exit 1 ;; # If expected argument omitted:
        *) usage_and_exit 1 ;; # If unknown (any other) option
    esac
done
shift "$((OPTIND-1))"

if [ ! "${bridge_id}" ]; then
  echo "$0: destination bridge ID is empty"; usage_and_exit 1;
elif [ ! "${processor_name}" ]; then
  echo "$0: processor name ID is empty"; usage_and_exit 1;
elif [ ! "${flow_file}" ]; then
  echo "$0: flow file is empty"; usage_and_exit 1;
elif [ ! -f "${flow_file}" ]; then
  echo "$0: flow file \"${flow_file}\" doesn't exist"; usage_and_exit 1;
fi

json_flow=$( yq e -o=json "$flow_file" )

set -x

curl -vvv "${MANAGER_URL:-"http://localhost:8080"}/api/smartevents_mgmt/v2/bridges/${bridge_id}/processors" \
  -H "Authorization: bearer ${OB_TOKEN}" \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  --data-raw "{\"name\":\"${processor_name}\",\"flows\":${json_flow}"
