#!/bin/bash

if [ -z "$SF_RAM" ]; then
  echo "SF_RAM is not set. Defaulting to 2G."
  SF_RAM="2G"
fi

if [ -z "$SF_JAR" ]; then
  echo "SF_JAR is not set. Using default jar file of this image."
  SF_JAR="/soulfire/soulfire.jar"
fi

echo "Starting SoulFire dedicated server..."
java -Xmx$SF_RAM $SF_JVM_FLAGS -XX:+EnableDynamicAgentLoading -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M -jar $SF_JAR
