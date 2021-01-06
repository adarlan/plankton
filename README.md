# Plankton

Plankton is a tool for running Container-Native CI/CD using [The Compose Specification](https://github.com/compose-spec/compose-spec/blob/master/spec.md).

Compose labels can be used to define the rules for pipelines, such as the step order, expressions to enable or disable operations, among other rules.

## Try it yourself

Create the file `plankton.compose.yaml` with the following content:

```yml
...
```

Run the pipeline using the `docker run` command:

```shell
docker run -it --rm -v $PWD:/workspace -v /var/run/docker.sock:/var/run/docker.sock --network host -p 1329:1329 adarlan/plankton
```

View the pipeline in your browser: [http://localhost:1329](http://localhost:1329)

## Label reference

* `plankton.timeout`
* `plankton.enable.if`
* `plankton.wait.success.of`
* `plankton.wait.failure.of`
* `plankton.wait.ports`
* `plankton.wait.files`

## Argument reference

* `--plankton.name`
* `--plankton.file`
* `--plankton.workspace`
* `--plankton.metadata`
* `--plankton.docker-host`
