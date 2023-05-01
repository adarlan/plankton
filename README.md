# Plankton: A Lightweight Pipeline Orchestration Tool

![Plankton's logo](docs/img/plankton-rocket.png)

Plankton is an open-source pipeline orchestration tool that leverages the power of containers to run each job within its own isolated environment.

With Plankton, you can define a pipeline configuration using a `plankton.yaml` file and execute it using a `docker run` command. Once the pipeline is running, you can track its progress using either the terminal or a web interface in your browser.

## Example Pipeline and Usage

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

- `image`: specifies the container image that the job should run on.
- `volumes`: mounts a local directory ./ as /usr/src/app within the container.
- `working_dir`: sets the working directory within the container to /usr/src/app.
- `entrypoint`: specifies the command to run within the container. In this example, each job runs a loop using the for command to output a message and then wait for 1 second before repeating, for a certain number of iterations specified by the seq command.
- `depends_on`: used in the build and deploy jobs to indicate that they depend on the successful completion of the previous job in the pipeline. In this case, the build job depends on the test job, and the deploy job depends on the build job.

> View all job properties in the [Plankton Pipeline Configuration Reference](docs/img/pipeline-configuration.md).

## Run Your Pipeline with Docker

To run the pipeline, execute the following command:

```shell
docker run -it -v /var/run/docker.sock:/var/run/docker.sock -v $PWD:/usr/src/app -w /usr/src/app -p 1329:1329 adarlan/plankton
```

- `-v /var/run/docker.sock:/var/run/docker.sock`: This option mounts the Docker socket file on the host machine inside the container, allowing the container to communicate with the Docker engine running on the host. This is necessary for running Docker commands from within the container. If you are concerned (and rightly so) with running third-party containers with access to the Docker host, you can easily [try Plankton using Play-with-Docker](docs/play-with-docker.md).
- `-v $PWD:/usr/src/app`: This option mounts the current directory (`$PWD`) on the host machine as a volume inside the container at `/usr/src/app`. This allows the container to access the files in the current directory, which is where the `plankton.yaml` file is located.
- `-w /usr/src/app`: This option sets the working directory for the container to `/usr/src/app`. This means that when the container starts, it will start in the directory where the `plankton.yaml` file is located.
- `-p 1329:1329`: This option maps port `1329` on the host machine to port `1329` inside the container. This allows you to access the Plankton web interface from your host machine's web browser.

## Web-Based User Interface

You can track the progress of your pipeline in your browser by opening [http://localhost:1329](http://localhost:1329).

![Web-based user interface](docs/img/plankton-web.png)

## Display Pipeline Logs on Terminal

You can view real-time logs of your pipeline jobs in your terminal.

![Pipeline logs on terminal](docs/img/plankton-logs.png)

## Built on the Compose Specification

You may have noticed that the `plankton.yaml` file is configured similarly to a Docker Compose file, but instead of defining `services`, in the Plankton file you define `jobs`.

This configuration format is defined by the [Compose Specification](https://github.com/compose-spec/compose-spec/blob/master/spec.md), which establishes a standard for the definition of multi-container systems.

By using the Compose Specification, Plankton can benefit from a well-defined specification that is known to many people and maintained by a global community.

## Dogfooding: Plankton Uses Itself!

In the Plankton repository there is a `plankton.yaml` file, where is configured a pipeline to build, test and deploy the Plankton itself.

See how it looks in the web interface:

![Plankton using itself](docs/img/plankton-using-itself.png)

## Learn More About Plankton

Check out the following resources to find out more about Plankton:

- [Plankton Pipeline Configurarion Reference](docs/pipeline-configuration.md)
- [Plankton CLI Reference](docs/cli-reference.md)
- [Using Plankton to Build and Test a Web Application](examples/testing-web-application/)
- [Using Plankton to Test, Build and Deploy Itself](docs/using-itself.md)
- [Running Parallel Jobs with Plankton](examples/running-parallel-jobs/)
- [Running Plankton Jobs in a Sandbox](docs/sandbox.md)
- [Try Plankton Using Play-with-Docker](docs/play-with-docker.md)

## Contribute to Plankton

I welcome contributions from anyone interested in improving Plankton. If you have any ideas, suggestions, or bug reports, please feel free to create an issue on the GitHub repository. If you'd like to contribute code, please fork the repository, create a new branch, and submit a pull request with your changes. I appreciate all contributions and will do my best to review them as quickly as possible.
