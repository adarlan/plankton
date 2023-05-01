# Plankton

![plankton-rocket.png](docs/img/plankton-rocket.png)

Plankton is an open-source CI/CD tool that leverages the power of containers to run each pipeline job within its own isolated environment.

With Plankton, you can define a pipeline configuration using a `plankton.yaml` file and execute it using a `docker run` command. Once the pipeline is running, you can track its progress using either the terminal or a web interface in your browser.

## Example

Here's an example of a `plankton.yaml` file:

```yaml
jobs:

  test:
    image: alpine
    volumes:
      - ./:/usr/src/app
    working_dir: /usr/src/app
    entrypoint:
      - for i in $(seq 1 5); do
      -   echo "Testing..."
      -   sleep 1
      - done

  build:
    depends_on: test
    image: alpine
    volumes:
      - ./:/usr/src/app
    working_dir: /usr/src/app
    entrypoint:
      - for i in $(seq 1 7); do
      -   echo "Building..."
      -   sleep 1
      - done

  deploy:
    depends_on: build
    image: alpine
    volumes:
      - ./:/usr/src/app
    working_dir: /usr/src/app
    entrypoint:
      - for i in $(seq 1 3); do
      -   echo "Deploying..."
      -   sleep 1
      - done
```

- `image`: specifies the Docker image that the job should run on.
- `volumes`: mounts a local directory ./ as /usr/src/app within the container.
- `working_dir`: sets the working directory within the container to /usr/src/app.
- `entrypoint`: specifies the command to run within the container. In this example, each job runs a loop using the for command to output a message and then wait for 1 second before repeating, for a certain number of iterations specified by the seq command.
- `depends_on`: used in the build and deploy jobs to indicate that they depend on the successful completion of the previous job in the pipeline. In this case, the build job depends on the test job, and the deploy job depends on the build job.

## Run the pipeline using Docker

To run the pipeline, execute the following command:

```shell
docker run -it -v /var/run/docker.sock:/var/run/docker.sock -v $PWD:/usr/src/app -w /usr/src/app -p 1329:1329 adarlan/plankton
```

- `-v /var/run/docker.sock:/var/run/docker.sock`: This option mounts the Docker socket file on the host machine inside the container, allowing the container to communicate with the Docker engine running on the host. This is necessary for running Docker commands from within the container. If you are concerned (and rightly so) with running third-party containers with access to the Docker host, you can easily [try Plankton using Play-with-Docker](docs/running-in-pwd.md).
- `-v $PWD:/usr/src/app`: This option mounts the current directory (`$PWD`) on the host machine as a volume inside the container at `/usr/src/app`. This allows the container to access the files in the current directory, which is where the `plankton.yaml` file is located.
- `-w /usr/src/app`: This option sets the working directory for the container to `/usr/src/app`. This means that when the container starts, it will start in the directory where the `plankton.yaml` file is located.
- `-p 1329:1329`: This option maps port `1329` on the host machine to port `1329` inside the container. This allows you to access the Plankton web interface from your host machine's web browser.

### Web interface

You can track the progress of your pipeline in your browser by opening [http://localhost:1329](http://localhost:1329).

![plankton-web.png](docs/img/plankton-web.png)

You can also follow the logs in your terminal:

![plankton-logs.png](docs/img/plankton-logs.png)

## Plankton is based on the Compose Specification

You may have noticed that the `plankton.yaml` file
is configured similarly to a Docker Compose file,
but instead of defining `services`, in the Plankton file you define `jobs`.

This configuration format is defined by the
[Compose Specification](https://github.com/compose-spec/compose-spec/blob/master/spec.md),
which establishes a standard for the definition of multi-container systems.

By using the Compose Specification, Plankton can benefit from a well-defined specification that is known to many people and maintained by a global community.

## Plankton uses itself

In the Plankton repository there is a `plankton.yaml` file,
where is configured a pipeline to build, test and deploy the Plankton itself.

See how it looks in the web interface:

![plankton-using-itself.png](docs/img/plankton-using-itself.png)

## Learn more

Check out the following resources to find out more about Plankton:

- [Plankton CLI reference](docs/runner-configuration.md)
- [Plankton pipeline configurarion reference](docs/pipeline-configuration.md)
- [Using Plankton to build and test a web application](examples/testing-web-application/)
- [Try Plankton in Play-with-Docker](docs/running-in-pwd.md)
- [Running parallel jobs with Plankton](examples/running-parallel-jobs/)
- [Using Plankton to test, build and deploy itself](docs/building-itself.md)
- [Running Plankton jobs in a sandbox](docs/sandbox.md)

<!-- ## Contributing -->

<!-- Plankton is a Spring-Boot Java application, but its core package is pure Java. -->
