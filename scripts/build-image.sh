#!/bin/bash

projectVersion=$(grep '^maven_version=' gradle.properties | cut -d'=' -f2)
commitSha=$(git rev-parse --short HEAD)
echo "Building image for commit $commitSha and version $projectVersion"

docker buildx build \
  --push \
  --platform=linux/amd64,linux/arm64 \
  -t alexprogrammerde/soulfire:$commitSha \
  -t alexprogrammerde/soulfire:$projectVersion \
  -t alexprogrammerde/soulfire:latest \
  .
