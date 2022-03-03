#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

http -v $MANAGER_URL/api/v1/bridges/$BRIDGE_ID/processors/$PROCESSOR_ID Authorization:"$OB_TOKEN"