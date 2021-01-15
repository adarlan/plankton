FROM docker:19.03
RUN apk update
RUN apk add bash docker-compose openjdk11-jre
RUN apk add git curl jq gettext

ENTRYPOINT [ "java", "-jar", "/plankton/runner.jar" ]

EXPOSE 1329
WORKDIR /workspace

COPY target/plankton-runner.jar /plankton/runner.jar
