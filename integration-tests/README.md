# integration-tests

Module contains integration tests verifying end to end functionality in real cluster. The tests are executed using command `mvn clean verify -Pcucumber -Devent-bridge.manager.url=http://<minikube IP>/manager -Dkey-cloak.url=http://<minikube IP>:30007/auth/realms/event-bridge-fm`.

Optionally the `key-cloak.url` parameter can be omitted by setting `OB_TOKEN` environment variable. Just a note, the `OB_TOKEN` value has to be the token value itself, without `Bearer ` prefix.  

Prerequisites for running the tests:
- Cluster is up and running
- All components are deployed using kustomize overlays
- Configure 'event-bridge.manager.url' and 'key-cloak.url' in pom.xml or set using maven parameters mentioned above
 
 By Default test runs with user name "kermit" and can be run the tests in different credentials and client config, the parameters needs to configure in integration-tests/pom.xml
  
    