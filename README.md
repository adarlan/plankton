# Dockerflow

Dockerflow is a tool that runs Docker containers in a workflow.
It can be used to make CI/CD pipelines or any kind of workflow
you mind using a Docker Compose configuration file.

## Getting started

If you already know the Docker Compose file format,
Dockerflow is quite simple.

All you need to know is a few labels to use in you Docker Compose file.

Example:

```yml
example...
```

[See more examples...](dockerflow-examples)

## Labels

* `dockerflow.start.when`
* `dockerflow.stop.when`
* `dockerflow.timeout`
* `dockerflow.enable.if`
* `dockerflow.wait.success.of`
* `dockerflow.wait.ports`
* `dockerflow.wait.files`

## Install

```shell
cd core
mvn install
cd ..
docker build -t dockerflow:mvn -f mvn.Dockerfile ~/.m2
docker build -t dockerflow:local -f on-local/Dockerfile on-local
```

Na versão cloud, o usuário pode cadastrar arquivos e definir expressões para determinar em quais
situações cada arquivo fica disponível.
Esses arquivos serão simplesmente colocados na workspace.
Podem ser arquivos de variáveis ou qualquer outro tipo de arquivo.
Para usar os arquivos, basta referenciá-los no docker-compose.
Cada arquivo tem um path (relativo à workspace), não precisa ser necessariamente na raiz.
Esses arquivos substituem os da workspace, se houver.

## Test

```shell
./mvnw spring-boot:run -Dspring-boot.run.arguments="--dockerflow.environment=example.env --dockerflow.file=example.docker-compose.yml"
```

```shell
docker run -it --network host -v /var/run/docker.sock:/var/run/docker.sock -v $PWD:/workspace --workdir /workspace runner:dev -Dspring-boot.run.arguments="--dockerflow.environment=example.env --dockerflow.file=example.docker-compose.yml"
```

## Run

```shell
docker run -it -p 1329:1329 -v /var/run/docker.sock:/var/run/docker.sock -v $PWD:/workspace --workdir /workspace --network host runner:dev --dockerflow.environment=example.env --dockerflow.file=example.docker-compose.yml
```

```yml
  dockerflow:
    image: runner:dev
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ${DOCKERFLOW_WORKSPACE}:/workspace
    ports:
      - 1329:1329
    command: -Dspring-boot.run.arguments="--dockerflow.name=${DOCKERFLOW_NAME} --dockerflow.workspace=/workspace --dockerflow.environment=${DOCKERFLOW_ENVIRONMENT} --dockerflow.file=${DOCKERFLOW_FILE} --dockerflow.metadata=${DOCKERFLOW_METADATA}"

  d0ck3rfl0w:
    image: runner:dev
    volumes:
      - .:/workspace
      - /var/run/docker.sock:/var/run/docker.sock
    networks:
      - host
    ports:
      - 1329:1329
    command:
      - --dockerflow.env=example.env
      - --dockerflow.file=example.docker-compose.yml
    #environment:
    #  DOCKER_HOST: tcp://docker:2375
```

```shell
docker-compose up dockerflow
```

## Labels

| Label | Example | Description |
| ----- | ------- | ----------- |
| `dockerflow.timeout` |  |  |
| `dockerflow.enable.if` |  |  |
| `dockerflow.wait.[SERVICE].status` |  | **Deprecated** |
| `dockerflow.wait.success` | `download` | Wait for a list of services to be finished with success. |
| `dockerflow.wait.failure` | `download` | Wait for a list of services to be finished with failure. |
| `dockerflow.wait.ports` | `3306` | Wait for a list of published ports. |
| `dockerflow.wait.files` | `flags/downloaded` | Wait for a list of files. It's recommended to use file paths relative to the working directory (`./relative/path/to/file` or (`relative/path/to/file`). You can use absolute file paths (`/absolute/path/to/file`), but keep in mind that it will be checked from within Dockerflow container, so make volume mapping properly. |
| `dockerflow.wait.human` | `true` ? |  |

## Test

Build runner:dev

```shell
docker build -t runner:dev --file dev.Dockerfile ~/.m2
```

Start Dockerflow

```shell
bash dockerflow start
```

Follow Dockerflow logs

```shell
bash dockerflow follow
```

Stop Dockerflow

```shell
bash dockerflow stop
```

## Endpoints

/PROJECT/REF
Mostra todos os pipelines daquele REF, do mais recente pro mais antigo

## Hello World Tutorial

Download `dockerflow`:

```shell
curl https://raw.githubusercontent.com/adarlan/dockerflow/master/dockerflow -o dockerflow
```

Give permission to execute `dockerflow`:

```shell
chmod +x dockerflow
```

Create `dockerflow.docker-compose.yml`:

```yml
version: '3.7'

services:

  dockerflow:
  test:
  build:
  deploy:
```

Start Dockerflow

```shell
./dockerflow start
```
