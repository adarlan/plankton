# Dockerflow

Dockerflow is a tool that runs Docker containers in a workflow.
It can be used to make CI/CD pipelines or any kind of workflow
you mind using a Docker Compose configuration file.

If you already know Docker and the Docker Compose file format,
Dockerflow is quite simple.
All you need to know is a few labels to use in your Docker Compose file.

## Requirements

* Docker (version ???)

## Try it yourself

Create a file called `dockerflow.docker-compose.yml`
with the following content:

```yml
...
```

> The labels used here means that...

Run with Docker:

```shell
docker run -it --rm -v $PWD:/workspace -v /var/run/docker.sock:/var/run/docker.sock --network host -p 1329:1329 adarlan/dockerflow
```

Track in your browser: [http://localhost:1329](http://localhost:1329)

[See more examples...](examples)

## Labels

* `dockerflow.timeout`
* `dockerflow.enable.if`
* `dockerflow.wait.success.of`
* `dockerflow.wait.failure.of`
* `dockerflow.wait.ports`
* `dockerflow.wait.files`

## Arguments

* `--dockerflow.name`
* `--dockerflow.file`
* `--dockerflow.workspace`
* `--dockerflow.metadata`
* `--dockerflow.docker-host`
