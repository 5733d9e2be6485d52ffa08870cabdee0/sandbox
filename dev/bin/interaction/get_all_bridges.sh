#!/bin/sh

source "$(dirname "${BASH_SOURCE[0]}")/common.sh"

http -v $MANAGER_URL/api/v1/bridges Authorization:"$OB_TOKEN"