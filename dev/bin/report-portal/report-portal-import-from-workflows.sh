#!/bin/bash
set -e

# Import test result from Github workflow to Report Portal.
#
# Usage: report-portal-import-from-workflows.sh repo_owner, repository, workflow, launch_name
# Expected environment variables
# GITHUB_TOKEN ... token with access to workflow run artifacts
# REPORT_PORTAL_URL ... Report Portal URL
# REPORT_PORTAL_TOKEN ... Report Portal token
# REPORT_PORTAL_PROJECT ... Report Portal project

check_env_variable_exists() {
    env_variable_name=$1
    if [[ -z "${!env_variable_name}" ]]; then
        echo "Error: $env_variable_name environmental variable must be set" 1>&2;
        exit 1
    fi
}

import_junit() {
    junit_file="$1"
    launch_id=$(curl -s -X POST "$REPORT_PORTAL_URL/api/v1/$REPORT_PORTAL_PROJECT/launch/import" \
                    -H "accept: */*" -H  "Content-Type: multipart/form-data" \
                    -H "Authorization: bearer $REPORT_PORTAL_TOKEN" \
                    -F "file=@$junit_file;type=application/zip")
    launch_id=$(sed 's/.*Launch with id = \([0-9a-z-]*\) .*/\1/' <<< "$launch_id")
    echo "$launch_id"
}

was_run_imported() {
    id=$1
    query="filter.has.compositeAttribute=run_id:$id&filter.eq.name=$launch_name"
    total_elements=$(curl -s -X GET \
                    "$REPORT_PORTAL_URL/api/v1/$REPORT_PORTAL_PROJECT/launch?$query" \
                    -H "Authorization: Bearer $REPORT_PORTAL_TOKEN" \
                    -H "Accept: application/vnd.github+json" \
                    | jq -r '.page.totalElements')
    if [[ "$total_elements" -eq "0" ]]; then
        return 1
    else
        return 0
    fi
}

check_env_variable_exists "GITHUB_TOKEN"
check_env_variable_exists "REPORT_PORTAL_URL"
check_env_variable_exists "REPORT_PORTAL_TOKEN"
check_env_variable_exists "REPORT_PORTAL_PROJECT"

if [[ "$#" != 4 ]]; then
    echo "Error: exactly 4 parameters are expected (owner, repo, workflow, launch-name)" 1>&2;
    exit 1
fi

owner="$1"
repo="$2"
# jq -Rr @uri will encode forbidden characters from input string to characters acceptable in ulr
workflow=$(jq -Rr @uri <<< "$3")
launch_name="$4"

if [ -z "$JUNIT_TMP_DIR" ]; then
    JUNIT_TMP_DIR="$(pwd)/junit_tmp_dir"
    echo "JUNIT_TMP_DIR environment variable was not specified. Using $JUNIT_TMP_DIR"
fi
mkdir -p "$JUNIT_TMP_DIR"
tmp_directory_parent="$JUNIT_TMP_DIR/$owner-$repo-$workflow"

workflow_runs=$(curl -s \
                -H "Authorization: Bearer $GITHUB_TOKEN" \
                -H "Accept: application/vnd.github+json" \
                "https://api.github.com/repos/$owner/$repo/actions/workflows/$workflow/runs")

workflow_ids=$(jq '.workflow_runs[].id' <<< "$workflow_runs")

mkdir "$tmp_directory_parent"
for id in $workflow_ids; do
    if ! was_run_imported "$id"; then
        echo "Importing results from workflow run: $id"
        junit_url=$(curl -s \
            -H "Authorization: Bearer $GITHUB_TOKEN" \
            -H "Accept: application/vnd.github+json" \
            "https://api.github.com/repos/$owner/$repo/actions/runs/$id/artifacts" \
            | jq -r '.artifacts[] | select(.name=="junit-test-results") | .archive_download_url')
        if [ -n "$junit_url" ]; then
            junit_url=$(curl -sSI \
                        -H "Authorization: Bearer $GITHUB_TOKEN" \
                        -H "Accept: application/vnd.github+json" \
                        "$junit_url" \
                        | grep "location:" \
                        | sed 's/.*: \(.*\)/\1/' \
                        | tr -d '\r' \
                        | tr -d '\n')
            if [ -n "$junit_url" ]; then
                tmp_directory="$tmp_directory_parent/$id"
                mkdir "$tmp_directory"
                junit_zip="$tmp_directory/$launch_name.zip"
                curl -s "$junit_url" > "$junit_zip"
                launch_uuid=$(import_junit "$junit_zip")
                launch_id=$(curl -s -X GET "$REPORT_PORTAL_URL/api/v1/$REPORT_PORTAL_PROJECT/launch/uuid/$launch_uuid" \
                            -H  "accept: */*" \
                            -H  "Authorization: bearer $REPORT_PORTAL_TOKEN" \
                            | jq .id)
                curl -s -X PUT \
                    "$REPORT_PORTAL_URL/api/v1/$REPORT_PORTAL_PROJECT/launch/$launch_id/update" \
                    -H  "accept: */*" -H  "Content-Type: application/json" \
                    -H  "Authorization: bearer $REPORT_PORTAL_TOKEN" \
                    -d "{\"attributes\": [{\"key\": \"run_id\", \"value\": \"$id\"}]}" > /dev/null
            else
                echo "Warning: Can not load junit artifact. Artifact probably expired"
            fi
        else
            echo "Warning: missing junit for run: $id"
        fi
    else
        echo "Run was already imported: $id"
    fi
done
