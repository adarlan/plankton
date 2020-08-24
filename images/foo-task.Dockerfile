FROM alpine
RUN apk add bash
COPY foo-task /foo-task
RUN chmod +x /foo-task
ENTRYPOINT [ "/foo-task" ]
