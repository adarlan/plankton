# Plankton

Plankton is a Container-Native CI/CD tool based on [The Compose Specification](https://github.com/compose-spec/compose-spec/blob/master/spec.md).

## Getting Started

Follow this example to create a simple pipeline composed by 3 services:
`test`, `build` and `deploy`.

Create a `plankton.compose.yaml` file with the following content:

```yaml
services:

  test:
    image: alpine
    command: echo Testing...

  build:
    image: alpine
    command: echo Building...
    depends_on:
      - test

  deploy:
    image: alpine
    command: echo Deploying...
    depends_on:
      - build
```

Run the pipeline using the `docker run` command:

```shell
docker run -it --rm -v $PWD:/workspace -w /workspace -v /var/run/docker.sock:/var/run/docker.sock -p 1329:1329 adarlan/plankton
```

See the pipeline logs:

![Pipeline logs](screenshots/pipeline-logs.png)

View the pipeline in your browser: [http://localhost:1329](http://localhost:1329)

![Pipeline page](screenshots/pipeline-page.png)

## Profiles

Profiles is a Plankton specific top level key:

```yaml
profiles:
  PROFILE: STRING=REGEX

services:
  SERVICE:
    profiles:
      - PROFILE
```

## Plankton uses itself

On the plankton project directory there is a `plankton.compose.yaml` file,
where is configured a pipeline to:

* Test the Plankton code using Maven
* Build the `plankton.jar` file using Maven
* Test the `plankton.jar` file using a JRE (Java Runtime Environment)
* Build the Plankton container images (tags: `runner` and `sandbox`)
* Test the Plankton container images using Docker
* Push the Plankton container images to [Plankton registry](https://hub.docker.com/repository/docker/adarlan/plankton)
* Deploy the Plankton services on [https://plankton.services](https://plankton.services)

You can run this pipeline just executing `mvn spring-boot:run` inside the Plankton project directory.

## Benchmark

| Plankton | GitLab-CI |
| -------- | --------- |
| Use an open source language (`.compose.yaml`) | Use its own language (`.gitlab-ci.yml`) |
| Run pipeline locally | Need server |
| Build job image on pipeline runtime | Job image must already exist before start the pipeline |
| Use as job image an image built on previous job | Job image must already exist before start the pipeline |
| Pull ahead the job images | Pull when job start |
| Bind mounts from underlying file system to job container file system | ... |
| Containers can share data through bind mounts | Ugly cache feature |
| Containers can communicate through sockets | ... |
| A service is just another container that is stopped when no more jobs depend on it | A service is a specific configuration |
