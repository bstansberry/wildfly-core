#!/bin/sh

/opt/criu/criu restore --unprivileged -D /opt/jboss/wildfly/standalone/data/criu/dump --file-locks --shell-job --tcp-established --skip-file-rwx-check

exit 0
