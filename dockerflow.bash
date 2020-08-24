#!/bin/bash
set -eu

if [ ! -z ${1+x} ] && [ "${1}" = "--clear" ]; then
    docker-compose --file docker-compose.yml --project-name fooo down
else
    docker-compose --file docker-compose.yml --project-name fooo down

    #trap 'docker-compose --file docker-compose.yml --project-name fooo down --timeout 30' SIGTERM

    #docker-compose --file docker-compose.yml --project-name fooo build --pull --parallel

    docker-compose --file docker-compose.yml --project-name fooo \
    up --force-recreate --remove-orphans --abort-on-container-exit --exit-code-from dockerflow \
    dockerflow
fi
