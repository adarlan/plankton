FROM docker:19.03

RUN apk add docker-compose openjdk11-jre
RUN apk add bash curl jq gettext

WORKDIR /workspace
ENTRYPOINT [ "./mvnw", "spring-boot:run" ]
COPY repository /root/.m2/repository
