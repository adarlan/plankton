#!/bin/sh
set -ex

mkdir ~/.ssh
ssh-keyscan $SERVER_IP >~/.ssh/known_hosts
chmod 644 ~/.ssh/known_hosts
eval $(ssh-agent -s)
chmod 400 $PRIVATE_KEY
ssh-add $PRIVATE_KEY
echo $SERVER_IP >hosts
ansible-playbook -i hosts -u ubuntu ansible-playbook.yaml
