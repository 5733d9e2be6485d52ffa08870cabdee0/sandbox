#!/bin/sh

source "common.sh"

http -v $MANAGER_URL/api/v1/bridges/$BRIDGE_ID Authorization:"$OB_TOKEN"