#!/bin/bash

PREFIX=$(whoami)

export TODAY_BRIDGE_NAME="$PREFIX-$(date +%Y-%m-%d)-bridge"
export TODAY_PROCESSOR_NAME="$PREFIX-$(date +%Y-%m-%d)-processor"

CONFIGURE_SCRIPT_DIR_PATH=`dirname "${BASH_SOURCE[0]}"`

# load local environment variables if found
if [ -f "${CONFIGURE_SCRIPT_DIR_PATH}/environment" ]; then
  . "${CONFIGURE_SCRIPT_DIR_PATH}/environment"
  echo "Loaded local environment file"
fi

source "$(dirname "${BASH_SOURCE[0]}")/get-token.sh"