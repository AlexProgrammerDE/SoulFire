#!/bin/bash

if [ -z "$SF_RAM" ]; then
  echo "SF_RAM is not set. Defaulting to 2G."
  SF_RAM="2G"
fi

echo "Starting SoulFire dedicated server..."
java -Xmx$SF_RAM -XX:+EnableDynamicAgentLoading -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M -jar /soulfire/soulfire.jar
