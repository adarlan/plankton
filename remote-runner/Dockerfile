FROM docker:19.03
RUN apk update
RUN apk add bash docker-compose openjdk11-jre
RUN apk add git curl jq gettext

ENTRYPOINT [ "java", "-jar", "/plankton/remote-runner.jar" ]
COPY target/plankton-remote-runner.jar /plankton/remote-runner.jar
