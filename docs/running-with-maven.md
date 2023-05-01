# Running Plankton Using Maven

## It requires

- Maven
- Java
- Docker
- Bash
- [jq](https://stedolan.github.io/jq/)

<!-- - [gettext](https://www.gnu.org/software/gettext/) -->

## Run over itself

```shell
mvn spring-boot:run
```

## Run example project

```shell
mvn spring-boot:run -Dspring-boot.run.arguments="--workspace=examples/getting-started"
```
