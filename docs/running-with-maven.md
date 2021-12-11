# Plankton - Running with Maven

## It requires

- Maven
- Java
- Docker
- Bash
- [gettext](https://www.gnu.org/software/gettext/)
- [jq](https://stedolan.github.io/jq/)

## Run over itself

```shell
mvn spring-boot:run
```

To be able to push the Plankton images to the container registry,
you need to provide the registry credentials in the `plankton.env` file,
setting the following variables:

- `REGISTRY_USER`
- `REGISTRY_PASSWORD`

## Run example project

```shell
mvn spring-boot:run -Dspring-boot.run.arguments="--workspace=examples/getting-started"
```
