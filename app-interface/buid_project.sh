#!/bin/bash

# using quarkus.container-image.build to avoid issues on fleet shard jib config
mvn clean install -Dquickly -Dquarkus.container-image.build=false