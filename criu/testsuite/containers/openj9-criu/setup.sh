#!/bin/sh

# Based on $1, copy either a complete WF installation or a slimmed one into our 'wildfly' dir
rm -rf wildfly
mkdir wildfly

type=$1
cp -r ../../../wildfly/wildfly-core/criu/testsuite/dist/target/wildfly-${type}/* wildfly/

# Add the deployment
cp kitchensink.war wildfly/standalone/deployments

