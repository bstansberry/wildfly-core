#!/bin/sh

# hack to bump up the pid by 100
for i in {1..100}
do
    ./home/jboss/scripts/pidplus.sh
done

./home/jboss/wildfly/bin/standalone.sh --stability=experimental -b=0.0.0.0 < /dev/null &> /dev/null &

exit 0

