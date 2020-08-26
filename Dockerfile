FROM maven:3.6-jdk-11 AS build-stage
WORKDIR /app
COPY src /app/src
COPY pom.xml /app
RUN mvn --batch-mode -DskipTests package

FROM docker:19.03
RUN apk add bash docker-compose openjdk11-jre
WORKDIR /workspace
EXPOSE 1329
ENTRYPOINT [ "java", "-jar", "/dockerflow/run.jar" ]
COPY --from=build-stage /app/target/*.jar /dockerflow/run.jar
