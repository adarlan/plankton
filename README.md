# Dockerflow

Dockerflow is a tool that runs Docker containers in a workflow.
It can be used to make CI/CD pipelines using a Docker Compose configuration file.

If you already know the Docker Compose file format, Dockerflow is quite simple.
All you need to know are some labels to use in your Docker Compose file.

## System requirements

* Docker (version ???)

## Try it yourself

Create a file called `dockerflow.docker-compose.yml`
with the following content:

```yml
...
```

Run with Docker:

```shell
docker run -it --rm -v $PWD:/workspace -v /var/run/docker.sock:/var/run/docker.sock --network host -p 1329:1329 adarlan/dockerflow
```

Track in your browser: [http://localhost:1329](http://localhost:1329)

## Label reference

* `dockerflow.timeout`
* `dockerflow.enable.if`
* `dockerflow.wait.success.of`
* `dockerflow.wait.failure.of`
* `dockerflow.wait.ports`
* `dockerflow.wait.files`

## Argument reference

* `--dockerflow.name`
* `--dockerflow.file`
* `--dockerflow.workspace`
* `--dockerflow.metadata`
* `--dockerflow.docker-host`
