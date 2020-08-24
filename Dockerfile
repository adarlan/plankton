FROM maven:3.6-jdk-11 AS build-stage
COPY src /app/src
COPY pom.xml /app
WORKDIR /app
RUN mvn --batch-mode -DskipTests package

FROM docker:19.03
RUN apk add bash docker-compose openjdk11-jre

COPY docker-entrypoint /dockerflow/docker-entrypoint
RUN chmod +x /dockerflow/docker-entrypoint
ENTRYPOINT [ "/dockerflow/docker-entrypoint" ]

WORKDIR /workspace

COPY --from=build-stage /app/target/*.jar /dockerflow/run.jar
