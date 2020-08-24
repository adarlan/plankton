FROM docker:19.03
RUN apk add bash docker-compose openjdk11-jre 
WORKDIR /workspace
ENTRYPOINT [ "./mvnw", "spring-boot:run" ]
COPY maven-repository /root/.m2/repository
