#!/bin/bash

docker buildx build --push --platform linux/amd64 -t alexprogrammerde/soulfire .
