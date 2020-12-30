FROM docker:19.03
RUN apk add bash docker-compose openjdk11-jre
RUN apk add curl jq gettext

ENTRYPOINT [ "java", "-jar", "/dockerflow/local-runner.jar" ]
COPY target/dockerflow-local-runner.jar /dockerflow/local-runner.jar

EXPOSE 1329
WORKDIR /workspace
