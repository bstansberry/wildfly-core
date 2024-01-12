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

export WILDFLY_OVERRIDE_ENV_VARS=true

# Create an empty env-vars file to turn on WildFly CRIU's integration with J9's CRIUSupport.registerRestoreEnvFile
touch wildfly/standalone/configuration/criu-restore-env-vars.txt

# Include the required OpenJ9 CRIU settings in JAVA_OPTS
# Include the J9 setting telling it to read the WILDFLY_OVERRIDE_ENV_VARS env var
echo 'JAVA_OPTS="$JAVA_OPTS -XX:+EnableCRIUSupport -XX:-CRIUSecProvider -Dorg.eclipse.openj9.criu.ImmutableEnvVars=WILDFLY_OVERRIDE_ENV_VARS"' >> wildfly/bin/standalone.conf


exec wildfly/bin/standalone.sh --stability=experimental
