FROM docker:19.03

RUN apk update
RUN apk add openjdk11-jre
RUN apk add bash
RUN apk add docker-compose
RUN apk add jq
# RUN apk add git
# RUN apk add curl
# RUN apk add gettext

ENTRYPOINT [ "java", "-jar", "/plankton/plankton.jar" ]

EXPOSE 1329
WORKDIR /workspace

COPY target/plankton.jar /plankton/plankton.jar
