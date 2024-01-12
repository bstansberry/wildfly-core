FROM --platform=linux/amd64 registry.access.redhat.com/ubi8/ubi

RUN useradd -ms /bin/bash jboss

COPY openjdk-17-crac+6_linux-x64 /home/jboss/jdk-17
COPY --chown=jboss wildfly /home/jboss/wildfly
COPY --chown=jboss kitchensink.war /home/jboss/wildfly/standalone/deployments/
COPY --chown=jboss resource-policies.yml /home/jboss/resource-policies.yml

RUN chmod u+s /home/jboss/jdk-17/lib/criu

RUN echo 'JAVA_OPTS="$JAVA_OPTS -XX:CRaCCheckpointTo=/home/jboss/criu -Djdk.crac.resource-policies=/home/jboss/resource-policies.yml"' >> /home/jboss/wildfly/bin/standalone.conf

ENV JAVA_HOME=/home/jboss/jdk-17

#RUN ["/home/jboss/wildfly/bin/jboss-cli.sh", "--commands=embed-server,/extension=org.wildfly.extension.criu:add,/subsystem=criu:add"]
#RUN chown jboss:jboss /home/jboss/wildfly

USER jboss
CMD ["/home/jboss/wildfly/bin/standalone.sh", "--stability=experimental"]
#CMD ["/home/jboss/wildfly/bin/standalone.sh"]