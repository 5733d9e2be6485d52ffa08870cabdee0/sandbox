#!/bin/sh

source "common.sh"

http -v $MANAGER_URL/api/v1/bridges Authorization:"$OB_TOKEN"