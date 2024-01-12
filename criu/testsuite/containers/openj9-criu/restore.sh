#!/bin/sh

criu restore -D /home/jboss/wildfly/standalone/data/criu --file-locks --shell-job --tcp-established --unprivileged

exit 0
