# integration-tests Project

Module contains integration tests verifying end to end functionality in real cluster. The tests are executed using command `mvn clean verify -Pintegration'.

Prerequisites for running the tests:
    - Cluster is up and running 
    - All components are deployed using kustomize overlays 
    - Configure minikube ip in 'application.properties' file for 'event-bridge.manager.url' and 'key-cloak.url' fields
    
    