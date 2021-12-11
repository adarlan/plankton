# Plankton - Sandbox Mode

## Install the Sysbox Container Runtime

## Enable sandbox

The Plankton sandbox can be enabled by adding the `--sandbox` option.

Example:

```shell
docker run -it -v /var/run/docker.sock:/var/run/docker.sock -v $PWD:/app -w /app -p 1329:1329 adarlan/plankton --sandbox
```
