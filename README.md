![cover.png](docs/img/cover.png)

Plankton is a Container-Native CI/CD tool
based on the Compose Specification.

Just have a `plankton-compose.yaml` file containing the pipeline configuration
and execute a `docker run` command to start it.

## Compose Specification

The `plankton-compose.yaml` file is configured using the
[Compose Specification](https://github.com/compose-spec/compose-spec/blob/master/spec.md).

```yaml
services:
  job1: ...
  job2: ...
  job3: ...
```

It's the same configuration format used by Docker Compose,
but it's not exclusive to Docker Compose.

The Compose Specification
establishes a standard for the definition of multi-container systems.
So instead of creating a unique configuration format for a tool,
we can use a well-defined specification
that is known to many people
and maintained by a global community.

## Container-Native

The Compose Specification is defined as Container-Native.
That is, it allows the use of any container system that follows
the [Open Container Initiative](https://opencontainers.org/),
not only Docker containers.

At first, Plankton only supports Docker containers,
but the design patterns used in the code allow it to be extended by adding new adapters for other container systems.

## Run pipelines locally

Many CI/CD tools require you to push the source code to a remote repository
in order to run the pipeline on a server.

An interesting feature of Plankton is the possibility to run pipelines locally,
just executing a `docker run` command.

### Example

Create a `plankton-compose.yaml` file:

```yaml
services:

  list_cars:
    image: alpine
    entrypoint:
      - CARS="Saab Volvo BMW Fiat"
      - for car in $CARS; do echo $car; sleep 1; done

  list_fruits:
    depends_on: list_cars
    image: alpine
    entrypoint:
      - FRUITS="Banana Apple Mango"
      - for fruit in $FRUITS; do echo $fruit; sleep 1; done

  list_colors:
    depends_on: list_cars
    image: alpine
    entrypoint:
      - COLORS="Red Yellow Black Green Blue Brown White"
      - for color in $COLORS; do echo $color; sleep 1; done
```

Run the pipeline:

```shell
docker run -it \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $PWD:/app -w /app \
  -p 1329:1329 \
  adarlan/plankton
```

> It requires Docker installed.

> Note that Plankton requires access to a Docker host via `/var/run/docker.sock`.
> If you are concerned (and rightly so) with running third-party containers
> with access to your Docker host, you can try Plankton using
> [Play-with-Docker](https://labs.play-with-docker.com).

Follow the logs on your terminal:

![pipeline-logs.png](docs/img/pipeline-logs.png)

Open the web interface at [http://localhost:1329](http://localhost:1329)

![pipeline-page.png](docs/img/pipeline-page.png)

#### More examples

[Here](https://github.com/adarlan/plankton/tree/master/examples)
you can find some other use cases of Plankton.

## Run pipelines on a sandbox

Plankton does not have yet a server
to listen for changes in code repositories
and start the pipelines automatically.

But thinking about it as a future implementation,
Plankton already provides a sandbox for each pipeline,
improving container isolation.
It's done using the [Sysbox Container Runtime](https://github.com/nestybox/sysbox).

The Plankton sandbox can be enabled by adding the `--sandbox` option.

Example:

```shell
docker run -it \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v $PWD:/app -w /app \
  -p 1329:1329 \
  adarlan/plankton --sandbox
```

> It requires Sysbox installed.

## Plankton uses itself

In the Plankton repository there is a `plankton-compose.yaml` file,
where is configured a pipeline to build, test and deploy itself.

![using-itself-page.png](docs/img/using-itself-page.png)

In this case, does not make sense to run the pipeline using the `docker run` command,
because it will always use the previous version of Plankton to test the current version.

Instead, run the pipeline executing:

```shell
mvn spring-boot:run
```

> It requires Maven and Docker installed.

So it will run the current version of Plankton over itself.

To be able to push the Plankton images to the container registry,
you need to provide the registry credentials in the `plankton.env` file,
setting the following variables:

- `REGISTRY_USER`
- `REGISTRY_PASSWORD`

## Contribute

Plankton is an Open-Source and Container-Native CI/CD tool
with the potential to be more than a portfolio project
and become a real choice for CI/CD pipelines.

To contribute to Plankton,
please share this project,
send a pull request
or give a feedback to [@adarlan on Twitter](https://twitter.com/adarlan).
