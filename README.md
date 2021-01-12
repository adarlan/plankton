# Plankton

Plankton is a Container-Native CI/CD tool based on [The Compose Specification](https://github.com/compose-spec/compose-spec/blob/master/spec.md).

Labels can be used to define pipeline rules, such as the order in which pipeline services are run, expressions to enable or disable services, among other rules.
See the [Label Reference](#label-reference) section of this document.

## Getting Started

Follow this example to create a simple pipeline composed by 3 services:
`test`, `build` and `deploy`.

### Create a `plankton.compose.yaml` file with the following content

```yaml
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

### Run the pipeline using the `docker run` command

```shell
docker run -it --rm -v $PWD:/workspace -v /var/run/docker.sock:/var/run/docker.sock --network host adarlan/plankton
```

### See the pipeline logs

![Pipeline logs](screenshots/pipeline-logs.png)

### View the pipeline in your browser: [http://localhost:1329](http://localhost:1329)

![Pipeline page](screenshots/pipeline-page.png)

### See more [examples](examples)

> TODO add more screenshots

## Label Reference

| Label | Description |
| ----- | ----------- |
| `plankton.timeout` | Timeout for the service execution. |
| `plankton.enable.if` | Bash conditional expression to determine when a service should be enabled. All services are enabled by default. |
| `plankton.wait.success.of` | A list of services that this service must wait for success. |
| `plankton.wait.failure.of` | A list of services that this service must wait for failure. |
| `plankton.wait.ports` | A list of published ports the service must wait for. |

## Variable Reference

| Variable | Description |
| -------- | ----------- |
| `GIT_REF` | ... |

## Argument Reference

<!-- You can define arguments when running the pipeline through command line:

```shell
docker run [OPTIONS] adarlan/plankton --plankton.compose.file=cicd.compose.yaml --plankton.workspace=.
``` -->

<!-- ### Plankton config
### Docker config -->

| Argument | Description |
| -------- | ----------- |
| `--plankton.compose.file` | The path of the Compose file which contains the pipeline configuration. Defaults to `plankton.compose.yaml`. |
| `--plankton.workspace` | Defaults to `.` |
| `--plankton.metadata` | Defaults to `.plankton` |
| `--plankton.docker.host` | Defaults to `unix:///var/run/docker.sock` |

## Unsupported Compose Attributes

```txt
services.<SERVICE>.container_name
services.<SERVICE>.deploy
```
