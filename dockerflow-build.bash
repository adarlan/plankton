#!/bin/bash
set -ex

MAVEN_TAG="3.6-jdk-11"

CORE_LIBRARY="dockerflow-core-library"
LOCAL_RUNNER="dockerflow-local-runner"
REMOTE_RUNNER="dockerflow-remote-runner"

# build and install core-library jar
docker run -it --rm \
  -u $(id -u $USER):$(id -g $USER) \
  -v /etc/passwd:/etc/passwd:ro \
  -v $HOME/.m2/repository:$HOME/.m2/repository \
  -v $PWD/$CORE_LIBRARY:/workspace \
  -w /workspace \
  --entrypoint "" \
  maven:$MAVEN_TAG \
  mvn install

# build local-runner jar
docker run -it --rm \
  -u $(id -u $USER):$(id -g $USER) \
  -v /etc/passwd:/etc/passwd:ro \
  -v $HOME/.m2/repository:$HOME/.m2/repository \
  -v $PWD/$LOCAL_RUNNER:/workspace \
  -w /workspace \
  --entrypoint "" \
  maven:$MAVEN_TAG \
  mvn package

# build remote-runner jar
docker run -it --rm \
  -u $(id -u $USER):$(id -g $USER) \
  -v /etc/passwd:/etc/passwd:ro \
  -v $HOME/.m2/repository:$HOME/.m2/repository \
  -v $PWD/$REMOTE_RUNNER:/workspace \
  -w /workspace \
  --entrypoint "" \
  maven:$MAVEN_TAG \
  mvn package

docker build -t dockerflow:local-runner ${LOCAL_RUNNER}
docker build -t dockerflow:remote-runner ${REMOTE_RUNNER}
docker build -t dockerflow:remote-runner-sandbox ${REMOTE_RUNNER}-sandbox
