#!/bin/bash
set -ex

TEST_APPLICATION="demo-application-1"

# run the local-runner container
[ $RUNNER = "local" ] && docker run -it --rm \
  -v $PWD/dockerflow-tests/$TEST_APPLICATION:/workspace \
  -w /workspace \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --network host \
  dockerflow:local-runner \
  --dockerflow.name=$TEST_APPLICATION \
  --dockerflow.file=dockerflow.docker-compose.yml

# run the remote-runner container
[ $RUNNER = "remote" ] && docker run -it --rm \
  -v $PWD/dockerflow-tests/$TEST_APPLICATION:/workspace \
  -w /workspace \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --network host \
  dockerflow:remote-runner

if [ $RUNNER = "remote" ]; then
  docker stack rm dockerflow-runner || true
  sleep 5
  docker stop $(docker ps -a -q) || true
  sleep 5
  mkdir -p ~/.dockerflow/runner-1
  docker stack deploy --compose-file on-server/runners.docker-compose.yml dockerflow
  docker service logs --follow dockerflow_runner-1
fi
