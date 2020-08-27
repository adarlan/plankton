# Dockerflow Runner

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
