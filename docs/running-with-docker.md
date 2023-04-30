# Plankton - Running with Docker

```shell
docker run -it -v /var/run/docker.sock:/var/run/docker.sock -v $PWD:/app -w /app -p 1329:1329 adarlan/plankton
```

- `-v /var/run/docker.sock:/var/run/docker.sock` is because Plankton needs access to the Docker host
- `-v $PWD:/app` and `-w /app` is because Plankton needs access to the directory containing the `plankton.yaml` file
- `-p 1329:1329` is because Plankton provides a web interface, which you can open at `http://localhost:1329`
