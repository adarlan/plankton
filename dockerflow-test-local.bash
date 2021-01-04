#!/bin/bash
set -ex

TEST_APPLICATION="demo-1"

# run the local-runner container
docker run -it --rm \
  -v $PWD/dockerflow-examples/$TEST_APPLICATION:/workspace \
  -w /workspace \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --network host \
  dockerflow:local-runner \
  --dockerflow.name=$TEST_APPLICATION \
  --dockerflow.file=dockerflow.docker-compose.yml
