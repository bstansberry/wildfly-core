#!/bin/sh

# I had to do this manually via sudo
#setcap cap_checkpoint_restore+eip /usr/sbin/criu

# Use the local OpenJ9 installation
dir=$(pwd)
export JAVA_HOME=$dir/jdk-21.0.1+12

# Based on $1, copy either a complete WF installation or a slimmed one into our 'wildfly' dir
rm -rf wildfly
mkdir wildfly
type=$1
cp -r ../../../wildfly/wildfly-core/criu/testsuite/dist/target/wildfly-${type}/* wildfly/

# Add the deployment
cp kitchensink.war wildfly/standalone/deployments


# Include the required OpenJ9 CRIU settings in JAVA_OPTS
echo 'JAVA_OPTS="$JAVA_OPTS -XX:+EnableCRIUSupport -XX:-CRIUSecProvider"' >> wildfly/bin/standalone.conf


exec wildfly/bin/standalone.sh --stability=experimental
