#!/bin/bash

VERSION=$1

if [ -z "$VERSION" ]; then
  echo "Usage: $0 <version>"
  exit 1
fi

# Fetch version manifest
curl -s https://piston-meta.mojang.com/mc/game/version_manifest_v2.json > /tmp/manifest.json

# Find URL for version
URL=$(jq -r ".versions[] | select(.id == \"$VERSION\") | .url" /tmp/manifest.json)

if [ "$URL" == "null" ] || [ -z "$URL" ]; then
  echo "Version $VERSION not found"
  exit 1
fi

# Fetch version JSON
curl -s $URL > /tmp/version.json

# Extract libraries and format as Kotlin DSL api
jq -r '.libraries[].name' /tmp/version.json | while read lib; do
  if [[ "$lib" == *lwjgl* && "$lib" == *natives* ]] || [[ "$lib" == *org.apache.logging.log4j* ]] || [[ "$lib" == *io.netty* ]] || [[ "$lib" == *jtracy* && "$lib" == *natives* ]] || [[ "$lib" == *java-objc-bridge* ]] || [[ "$lib" == *org.ow2.asm* ]]; then
    continue
  fi
  echo "api(\"$lib\")"
done
