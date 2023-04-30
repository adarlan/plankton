# Plankton - Pipeline configuration

## Supported keys

### build

```yaml
jobs:
  SERVICE:
    build: CONTEXT
  SERVICE:
    build:
      context: CONTEXT
      dockerfile: DOCKERFILE
```

### command

```yaml
jobs:
  SERVICE:
    command: COMMAND
  SERVICE:
    command:
      - COMMAND
```

### credential_spec ???

```yaml
jobs:
  SERVICE:
    credential_spec: # ???
```

### depends_on

```yaml
jobs:
  SERVICE:
    depends_on:
      - SERVICE
  SERVICE:
    depends_on:
      SERVICE:
        condition: service_healthy
        condition: service_started
        condition: service_completed_successfully # [DEFAULT]
        condition: service_failed # [PLANKTON]
```

### entrypoint

```yaml
jobs:
  SERVICE:
    entrypoint: ENTRYPOINT
  SERVICE:
    entrypoint:
      - ENTRYPOINT
```

### environment

```yaml
jobs:
  SERVICE:
    environment:
      - VARIABLE=VALUE
  SERVICE:
    environment:
      VARIABLE: VALUE
```

### env_file

```yaml
jobs:
  SERVICE:
    env_file: ENV_FILE
  SERVICE:
    env_file:
      - ENV_FILE
```

### expose

```yaml
jobs:
  SERVICE:
    expose:
      - PORT
```

### extends

```yaml
jobs:
  SERVICE:
    extends:
      file: FILE
      service: SERVICE
```

### group_add

```yaml
jobs:
  SERVICE:
    group_add:
      - GROUP
```

### healthcheck

```yaml
jobs:
  SERVICE:
    healthcheck:
      test: TEST
      interval: INTERVAL
      timeout: TIMEOUT
      retries: RETRIES
      start_period: START_PERIOD
  SERVICE:
    healthcheck:
      disabled: true
```

### image

```yaml
jobs:
  SERVICE:
    image: IMAGE
```

### profiles

```yaml
jobs:
  SERVICE:
    profiles:
      - PROFILE
```

### scale

```yaml
jobs:
  SERVICE:
    scale: SCALE # [DEPRECATED]
```

### user

```yaml
jobs:
  SERVICE:
    user: USER
```

### volumes

```yaml
jobs:
  SERVICE:
    volumes:
      - type: bind
        source: SOURCE_PATH
        target: TARGET_PATH
        read_only: BOOLEAN
  SERVICE:
    volumes:
      - SOURCE_PATH:TARGET_PATH
      - SOURCE_PATH:TARGET_PATH:rw
      - SOURCE_PATH:TARGET_PATH:ro
```

### working_dir

```yaml
jobs:
  SERVICE:
    working_dir: WORKING_DIR
```

## Ignored keys

```yaml
# configs
# networks
# secrets
# services.SERVICE.blkio_config
# services.SERVICE.build.args
# services.SERVICE.build.cache_from
# services.SERVICE.build.labels
# services.SERVICE.build.shm_size
# services.SERVICE.cpu_count
# services.SERVICE.cpu_percent
# services.SERVICE.cpu_shares
# services.SERVICE.cpu_period
# services.SERVICE.cpu_quota
# services.SERVICE.cpu_rt_runtime
# services.SERVICE.cpu_rt_period
# services.SERVICE.cpus [DEPRECATED]
# services.SERVICE.cpuset
# services.SERVICE.cap_add
# services.SERVICE.cap_drop
# services.SERVICE.cgroup_parent
# services.SERVICE.configs
# services.SERVICE.container_name
# services.SERVICE.deploy
# services.SERVICE.device_cgroup_rules
# services.SERVICE.devices
# services.SERVICE.dns
# services.SERVICE.dns_opt
# services.SERVICE.dns_search
# services.SERVICE.domainname
# services.SERVICE.external_links
# services.SERVICE.extra_hosts
# services.SERVICE.hostname
# services.SERVICE.init [???]
# services.SERVICE.ipc
# services.SERVICE.isolation
# services.SERVICE.labels
# services.SERVICE.links
# services.SERVICE.logging
# services.SERVICE.network_mode
# services.SERVICE.networks
# services.SERVICE.mac_address
# services.SERVICE.mem_limit [DEPRECATED]
# services.SERVICE.mem_reservation [DEPRECATED]
# services.SERVICE.mem_swappiness
# services.SERVICE.memswap_limit
# services.SERVICE.oom_kill_disable
# services.SERVICE.oom_score_adj
# services.SERVICE.pid
# services.SERVICE.pid_limit
# services.SERVICE.platform [???]
# services.SERVICE.ports
# services.SERVICE.privileged
# services.SERVICE.pull_policy [???]
# services.SERVICE.read_only
# services.SERVICE.restart
# services.SERVICE.runtime [DEPRECATED]
# services.SERVICE.secrets
# services.SERVICE.security_opt [???]
# services.SERVICE.shm_size
# services.SERVICE.stdin_open
# services.SERVICE.stop_grace_period
# services.SERVICE.stop_signal
# services.SERVICE.sysctls
# services.SERVICE.tmpfs
# services.SERVICE.tty
# services.SERVICE.ulimits
# services.SERVICE.userns_mode
# services.SERVICE.volumes[i].type volume
# services.SERVICE.volumes[i].type tmpfs
# services.SERVICE.volumes[i].type npipe
# services.SERVICE.volumes[i].bind
# services.SERVICE.volumes[i].volume
# services.SERVICE.volumes[i].tmpfs
# services.SERVICE.volumes[i].consistency
# services.SERVICE.volumes_from
# version [DEPRECATED]
# volumes
```
