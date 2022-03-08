#!/bin/bash -e

########
# Stop the running local Fleet Shard
#
# First argument is mandatory and should be the quarkus project directory from root dir
########

project_dir=$1
if [ -z "${project_dir}" ]; then
    echo 'Please provide project directory to stop (examples: shard-operator, manager ...)'
    exit 1
fi

shift

. $(dirname "${BASH_SOURCE[0]}")/common.sh

current_dir=$(pwd)
cd ${root_dir}
jar_name=$(find ${project_dir} -name '*-dev.jar')
cd ${current_dir}

if [ -z "${jar_name}" ]; then
    echo "No jar found for project ${project_dir}"
    echo "The process might not be running anymore"
    exit 0
fi

echo "Kill local shard operator process(es)"
kill_process_with_name "${jar_name}"
echo "Stopped local shard operator process(es)"
