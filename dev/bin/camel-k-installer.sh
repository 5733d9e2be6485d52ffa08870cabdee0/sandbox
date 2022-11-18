#!/bin/bash

camel_k_version="1.10.1"

set -e

# check operating system
case $(uname | tr '[:upper:]' '[:lower:]') in
  linux*)
    host_os=linux
    ;;
  darwin*)
    host_os=mac
    ;;
  msys*)
    host_os=windows
    ;;
  cygwin*)
    host_os=windows
    ;;
  *)
    die "Unsupported operating system"
    ;;
esac

cli_url="https://downloads.apache.org/camel/camel-k/${camel_k_version}/camel-k-client-${camel_k_version}-${host_os}-64bit.tar.gz"
echo $cli_url
cli_file_name="kamel-${camel_k_version}"

if ! [ -f "$cli_file_name" ]; then
  curl "$cli_url" | tar xzvO -f - kamel > "$cli_file_name"
fi
chmod +x "$cli_file_name"

echo "Using Camel CLI v${camel_k_version} for ${host_os}"

echo "---- K8S NODE DESCRIPTION - PRE INSTALL ----"
kubectl describe nodes
echo "--------------------------------------------"

kubectl create namespace camel-k --dry-run=client -o yaml | kubectl apply -f -
"./${cli_file_name}" install -n camel-k --global --force -w -V $@

echo "---- K8S NODE DESCRIPTION - POST INSTALL ---"
kubectl describe nodes
echo "--------------------------------------------"
