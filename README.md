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
docker run -it --rm -v $PWD:/workspace -v /var/run/docker.sock:/var/run/docker.sock adarlan/plankton
```

See the pipeline logs:

![Pipeline logs](screenshots/pipeline-logs.png)

View the pipeline in your browser: [http://localhost:1329](http://localhost:1329)

![Pipeline page](screenshots/pipeline-page.png)

## CLI

```shell
./plankton [ARGS...]
```
