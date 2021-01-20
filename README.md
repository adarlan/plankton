# Plankton

Plankton is a Container-Native CI/CD tool based on [The Compose Specification](https://github.com/compose-spec/compose-spec/blob/master/spec.md).

Labels can be used to define pipeline rules, such as the order in which pipeline jobs are run, expressions to enable or disable jobs, among other rules.
See the [Label Reference](#label-reference) section of this document.

## Getting Started

Follow this example to create a simple pipeline composed by 3 jobs:
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
| `plankton.timeout` | Timeout for the job execution. |
| `plankton.enable.if` | Bash conditional expression to determine when a job should be enabled. All jobs are enabled by default. |
| `plankton.wait.success.of` | A list of jobs that this job must wait for success. |
| `plankton.wait.failure.of` | A list of jobs that this job must wait for failure. |
| `plankton.wait.ports` | A list of published ports the job must wait for. |

## Variable Reference

| Variable | Description |
| -------- | ----------- |
| `GIT_REF` | ... |

## Argument Reference

Arguments to configure Plankton.

Example:

```shell
docker run -it --rm -v $PWD:/workspace -v /var/run/docker.sock:/var/run/docker.sock --network host adarlan/plankton --plankton.compose.file=cicd.compose.yaml --plankton.docker.sandbox=true
```

### Running a single pipeline

| Argument | Description |
| -------- | ----------- |
| `--plankton.compose.file` | The path of the Compose file which contains the pipeline configuration. Defaults to `plankton.compose.yaml`. |
| `--plankton.project.directory` | Defaults to `.` (current directory). |

> **Note**: Plankton runs within a container, so these paths are related to the container file system, not to the host file system. Be aware of this when mapping paths between them.

### Running multiple pipelines

| Argument | Description |
| -------- | ----------- |
| `--plankton.queue.url` | Each time the runner hit this URL, it expects a JSON on the format: `{"git_url": "...", "git_ref": "main"}`. |

### Docker configuration

| Argument | Description |
| -------- | ----------- |
| `--plankton.docker.host` | Defaults to `unix:///var/run/docker.sock` |
| `--plankton.docker.sandbox` | Boolean. Defaults to `false`. To enable this option, [Sysbox Container Runtime](https://github.com/nestybox/sysbox) is required. |

## Unsupported Compose Attributes

```txt
networks
services.<SERVICE>.container_name
services.<SERVICE>.deploy
```
