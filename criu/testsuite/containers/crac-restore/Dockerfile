FROM --platform=linux/amd64 registry.access.redhat.com/ubi8/ubi

RUN useradd -ms /bin/bash jboss

COPY openjdk-17-crac+6_linux-x64 /home/jboss/jdk-17
COPY --chown=jboss criu /home/jboss/criu

RUN chmod u+s /home/jboss/jdk-17/lib/criu

ENV JAVA_HOME=/home/jboss/jdk-17

USER jboss
CMD ["/home/jboss/jdk-17/bin/java", "-XX:CRaCRestoreFrom=criu"]