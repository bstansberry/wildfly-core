#!/bin/sh

# hack to bump up the pid by 100
for i in {1..100}
do
    ./opt/jboss/scripts/pidplus.sh
done

/opt/criu/criu check --unprivileged
./opt/jboss/wildfly/bin/standalone.sh --stability=experimental -b=0.0.0.0

exit 0

