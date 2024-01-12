#!/bin/sh

if [ -a "/opt/jboss/wildfly/standalone/data/criu/wildfly-checkpoint.txt" ]; then
  exec dumb-init --rewrite 15:2 -- "/opt/jboss/scripts/restore-rhel9.sh"
else
  ./opt/jboss/scripts/checkpoint-rhel9.sh
fi
