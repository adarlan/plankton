# Plankton - Runner configuration

## Options

### workspace

- Optional
- Default: `.`

> **Note**: When running Plankton from within a container,
> the workspace path is related to the container file system,
> not to the host file system.
> Be aware of this when mapping paths between them.

### file

- Optional
- Default: `plankton.yaml`

> **Note**: When running Plankton from within a container,
> the file path is related to the container file system,
> not to the host file system.
> Be aware of this when mapping paths between them.

### sandbox

- Optional
- Default: `false`

> To enable it, [Sysbox Container Runtime](https://github.com/nestybox/sysbox) mus be installed.

### spring.main.web-application-type

To disable the web interface, set it to `none`:

`spring.main.web-application-type=none`
