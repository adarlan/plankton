FROM alpine:latest

RUN apk update
RUN apk add docker
ENTRYPOINT [ "dockerd" ]

ARG BUILT_IN_IMAGES
COPY docker-pull /usr/bin/
RUN chmod +x /usr/bin/docker-pull
RUN docker-pull $BUILT_IN_IMAGES
RUN rm /usr/bin/docker-pull
