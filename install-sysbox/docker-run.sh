#!/bin/sh
set -eux

docker run -it --rm \
    -v $PWD:/wd \
    -w /wd \
    -e SERVER_IP=$1 \
    -v $2:/private_key \
    -e PRIVATE_KEY=/private_key \
    adarlan/ansible sh ansible-playbook.sh
