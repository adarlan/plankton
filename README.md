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
