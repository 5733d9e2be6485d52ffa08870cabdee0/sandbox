#!/bin/sh

script_dir_path=$(dirname "${BASH_SOURCE[0]}")

PREFIX=$(whoami)

export TODAY_BRIDGE_NAME="${PREFIX}-$(date +%Y-%m-%d)-bridge"
export TODAY_PROCESSOR_NAME="${PREFIX}-$(date +%Y-%m-%d)-processor"

. ${script_dir_path}/../common.sh

# load local environment variables if found
retrieve_full_env

# load environment if found
if [ -f "${dev_config_dir}/interactionconfig" ]; then
. "${dev_config_dir}/interactionconfig"
fi

function get_token {
    echo "${INTERACTION_OB_TOKEN}"
}

function check_token {
  if [ -z "$(get_token)" ]; then
    die "ERROR: No token defined. Please run the `$(dirname "${BASH_SOURCE[0]}")/get-token.sh` script first."
  fi
}