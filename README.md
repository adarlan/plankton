# Dockerflow

Dockerflow is a tool for running CI/CD pipelines using [The Compose Specification](https://github.com/compose-spec/compose-spec/blob/master/spec.md).

Some labels can be used to define the rulepipeline rules, such as the order of the steps, step timeout, expressions to enable or disable a step, among others.

## Try it yourself

Create the file `dockerflow.compose.yaml` with the following content:

```yml
...
```

Run using the `docker run` command:

```shell
docker run -it --rm -v $PWD:/workspace -v /var/run/docker.sock:/var/run/docker.sock --network host -p 1329:1329 adarlan/dockerflow
```

View the pipeline in your browser: [http://localhost:1329](http://localhost:1329)

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
