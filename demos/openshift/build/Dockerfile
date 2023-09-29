FROM registry.access.redhat.com/ubi9/openjdk-17:latest as builder

COPY pom.xml pom.xml
COPY .mvn .mvn
COPY src src

RUN mvn verify

FROM registry.redhat.io/ocp-tools-4/jenkins-rhel8:v4.11.0-1686832830.1693366259
COPY --from=builder /home/default/target/tkn-client-plugin.hpi /opt/openshift/plugins/tkn-client-plugin.jpi
