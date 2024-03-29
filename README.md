# Plankton

Plankton is a Container-Native CI/CD tool based on [The Compose Specification](https://github.com/compose-spec/compose-spec/blob/master/spec.md).

## Getting started

Follow this example to create a simple web application
and configure a Plankton pipeline to build and deploy it.

Create a `Dockerfile` with the following content:

```Dockerfile
FROM nginx
RUN echo "Hello Plankton" > /usr/share/nginx/html/index.html
```

Create a `plankton.compose.yaml` file with the following content:

```yaml
services:

  build:
    image: docker
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - .:/app
    working_dir: /app
    entrypoint: docker build -t hello-plankton .

  deploy:
    depends_on: build
    image: docker
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
    entrypoint: docker run -d hello-plankton
```

Run the pipeline using the `docker run` command:

> It requires Docker installed.

```shell
docker run -it -v /var/run/docker.sock:/var/run/docker.sock -v $PWD:/app -w /app -p 1329:1329 adarlan/plankton
```

See the pipeline logs:

![Pipeline logs](screenshots/pipeline-logs.png)

View the pipeline in your browser: [http://localhost:1329](http://localhost:1329)

![Pipeline page](screenshots/pipeline-page.png)

## Plankton uses itself

On the Plankton project directory there is a `plankton.compose.yaml` file,
where is configured a pipeline to:

* Build the `plankton.jar` file
* Build the container images: `runner` and `sandbox`
* Test the container images using the [examples](examples)
* Push the container images to [this registry](https://hub.docker.com/repository/docker/adarlan/plankton)

You can run this pipeline just executing `mvn spring-boot:run` inside the Plankton project directory.

It requires Maven and Docker installed.

It also requires a `plankton.env` file with the following variables,
which will be used to push the container images into de registry:

* `DOCKER_REGISTRY_USER`
* `DOCKER_REGISTRY_PASSWORD`
