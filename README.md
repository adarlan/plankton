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

| Label | Description | Example |
| ----- | ----------- | ------- |
| `plankton.timeout` | Timeout for the service execution. | [examples/timeout](examples/timeout) |
| `plankton.enable.if` | Expression to enable the service. All services are enabled by default. | [examples/enable.if](examples/enable.if) |
| `plankton.wait.success.of` | A list of services that this service must wait for success. | [examples/wait.success.of](examples/wait.success.of) |
| `plankton.wait.failure.of` | A list of services that this service must wait for failure. | [examples/wait.failure.of](examples/wait.failure.of) |
| `plankton.wait.ports` | A list of published ports the service must wait for. | [examples/wait.ports](examples/wait.ports) |

## Argument Reference

| Argument | Description |
| -------- | ----------- |
| `--plankton.compose.file` | Defaults to `plankton.compose.yaml` |
| `--plankton.workspace` | Defaults to `.` |
| `--plankton.metadata` | Defaults to `.plankton` |
| `--plankton.docker.host` | Defaults to `unix:///var/run/docker.sock` |
