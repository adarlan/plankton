FROM docker:19.03

RUN apk update
RUN apk add openjdk11-jre
RUN apk add bash
RUN apk add gettext
RUN apk add jq

ENTRYPOINT [ "java", "-jar", "/plankton/plankton.jar" ]
EXPOSE 1329

COPY target/plankton.jar /plankton/plankton.jar
