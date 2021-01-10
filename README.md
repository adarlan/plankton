# Plankton

Plankton is a Container-Native tool for running CI/CD pipelines using [The Compose Specification](https://github.com/compose-spec/compose-spec/blob/master/spec.md).

Compose labels can be used to define pipeline rules, such as the order of services, expressions to enable or disable services, among other rules.

## Example

Create a file called `plankton.compose.yaml` with the following content:

```yml
version: "3.7"

services:

  test:
    image: alpine
    command: echo Testing...

  build:
    image: alpine
    command: echo Building...
    labels:
      plankton.wait.success.of: test

  deploy:
    image: alpine
    command: echo Deploying...
    labels:
      plankton.wait.success.of: build
```

Run the pipeline using the `docker run` command:

```shell
docker run -it --rm -v $PWD:/workspace -v /var/run/docker.sock:/var/run/docker.sock --network host adarlan/plankton
```

View the pipeline in your browser: [http://localhost:1329](http://localhost:1329)

## Label Reference

* `plankton.timeout`
* `plankton.enable.if`
* `plankton.wait.success.of`
* `plankton.wait.failure.of`
* `plankton.wait.ports`

## Argument Reference

* `--plankton.compose.file` - defaults to `plankton.compose.yaml`
* `--plankton.workspace` - defaults to `.`
* `--plankton.metadata` - defaults to `.plankton`
* `--plankton.docker.host` - defaults to `unix:///var/run/docker.sock`
