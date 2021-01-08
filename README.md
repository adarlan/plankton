# Plankton

Plankton is a tool for running Container-Native CI/CD using [The Compose Specification](https://github.com/compose-spec/compose-spec/blob/master/spec.md).

Compose labels can be used to define pipeline rules, such as the step order, expressions to enable or disable operations, among other rules.

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
* `plankton.disable.if`
* `plankton.wait.success.of`
* `plankton.wait.failure.of`
* `plankton.wait.ports`

## Argument reference

* `--plankton.pipeline.id` - Defaults to `YYYY-MM-DD...`
* `--plankton.compose.file` - Defaults to `plankton.compose.yaml`
* `--plankton.workspace` - Defaults to `/workspace`
* `--plankton.docker.metadata` - Defaults to `${plankton.workspace}/.plankton/${plankton.pipeline.id}/docker.metadata`
* `--plankton.docker.host` - Defaults to `unix:///var/run/docker.sock`
