FROM docker:19.03
RUN apk add bash docker-compose openjdk11-jre
RUN apk add git curl jq gettext

ENTRYPOINT [ "java", "-jar", "/dockerflow/remote-runner.jar" ]
COPY target/dockerflow-remote-runner.jar /dockerflow/remote-runner.jar
