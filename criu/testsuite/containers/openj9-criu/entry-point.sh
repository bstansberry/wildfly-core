#!/bin/sh

if [ -a "/home/jboss/wildfly/standalone/data/criu/wildfly-checkpoint.txt" ]; then
  exec dumb-init --rewrite 15:2 -- "/home/jboss/scripts/restore.sh"
else
  ./home/jboss/scripts/checkpoint.sh
fi
