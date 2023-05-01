# Plankton Pipeline Configuration Reference

<!-- Services or jobs?
The pipeline jobs are under the `services` key
because the Compose Specification defines the components as "services".
But, for CI/CD purposes, it would be better to call it "jobs". -->

## `build`

```yaml
jobs:
  JOB:
    build: CONTEXT
  JOB:
    build:
      context: CONTEXT
      dockerfile: DOCKERFILE
```

## `command`

```yaml
jobs:
  JOB:
    command: OPTION
  JOB:
    command:
      - OPTION1
      - OPTION2
```

<!-- TODO ??? credential_spec

```yaml
jobs:
  JOB:
    credential_spec:
``` -->

## `depends_on`

```yaml
jobs:
  JOB:
    depends_on: OTHER
  JOB:
    depends_on:
      - OTHER1
      - OTHER2
  JOB:
    depends_on:
      OTHER:
        condition: job_completed_successfully
  JOB:
    depends_on:
      OTHER:
        condition: job_failed
  JOB:
    depends_on:
      OTHER:
        condition: service_healthy
```

<!--
service_started 
service_completed_successfully
service_failed
-->

## `entrypoint`

```yaml
jobs:
  JOB:
    entrypoint: COMMAND
  JOB:
    entrypoint:
      - COMMAND1
      - COMMAND2
```

## `environment`

```yaml
jobs:
  JOB:
    environment:
      - VARIABLE=VALUE
  JOB:
    environment:
      VARIABLE: VALUE
```

## `env_file`

```yaml
jobs:
  JOB:
    env_file: FILEPATH
  JOB:
    env_file:
      - FILEPATH1
      - FILEPATH2
```

## `expose`

```yaml
jobs:
  JOB:
    expose:
      - PORT
```

## `extends`

```yaml
jobs:
  JOB:
    extends: JOB
  JOB:
    extends:
      file: FILE
      job: JOB
```

## `group_add`

```yaml
jobs:
  JOB:
    group_add:
      - GROUP
```

## `healthcheck`

```yaml
jobs:
  JOB:
    healthcheck:
      test: TEST
      interval: INTERVAL
      timeout: TIMEOUT
      retries: RETRIES
      start_period: START_PERIOD
  JOB:
    healthcheck:
      disabled: true
```

## `image`

```yaml
jobs:
  JOB:
    image: IMAGE
```

## `profiles`

```yaml
jobs:
  JOB:
    profiles:
      - PROFILE
```

## `scale`

```yaml
jobs:
  JOB:
    scale: SCALE
```

<!-- [DEPRECATED] -->

## `user`

```yaml
jobs:
  JOB:
    user: USER
```

## `volumes`

```yaml
jobs:
  JOB:
    volumes:
      - type: bind
        source: SOURCE_PATH
        target: TARGET_PATH
        read_only: BOOLEAN
  JOB:
    volumes:
      - SOURCE_PATH:TARGET_PATH
      - SOURCE_PATH:TARGET_PATH:rw
      - SOURCE_PATH:TARGET_PATH:ro
```

## `working_dir`

```yaml
jobs:
  JOB:
    working_dir: WORKING_DIR
```

<!-- ## configs -->
<!-- ## networks -->
<!-- ## secrets -->
<!-- ## services.SERVICE.blkio_config -->
<!-- ## services.SERVICE.build.args -->
<!-- ## services.SERVICE.build.cache_from -->
<!-- ## services.SERVICE.build.labels -->
<!-- ## services.SERVICE.build.shm_size -->
<!-- ## services.SERVICE.cpu_count -->
<!-- ## services.SERVICE.cpu_percent -->
<!-- ## services.SERVICE.cpu_shares -->
<!-- ## services.SERVICE.cpu_period -->
<!-- ## services.SERVICE.cpu_quota -->
<!-- ## services.SERVICE.cpu_rt_runtime -->
<!-- ## services.SERVICE.cpu_rt_period -->
<!-- ## services.SERVICE.cpus [DEPRECATED] -->
<!-- ## services.SERVICE.cpuset -->
<!-- ## services.SERVICE.cap_add -->
<!-- ## services.SERVICE.cap_drop -->
<!-- ## services.SERVICE.cgroup_parent -->
<!-- ## services.SERVICE.configs -->
<!-- ## services.SERVICE.container_name -->
<!-- ## services.SERVICE.deploy -->
<!-- ## services.SERVICE.device_cgroup_rules -->
<!-- ## services.SERVICE.devices -->
<!-- ## services.SERVICE.dns -->
<!-- ## services.SERVICE.dns_opt -->
<!-- ## services.SERVICE.dns_search -->
<!-- ## services.SERVICE.domainname -->
<!-- ## services.SERVICE.external_links -->
<!-- ## services.SERVICE.extra_hosts -->
<!-- ## services.SERVICE.hostname -->
<!-- ## services.SERVICE.init [???] -->
<!-- ## services.SERVICE.ipc -->
<!-- ## services.SERVICE.isolation -->
<!-- ## services.SERVICE.labels -->
<!-- ## services.SERVICE.links -->
<!-- ## services.SERVICE.logging -->
<!-- ## services.SERVICE.network_mode -->
<!-- ## services.SERVICE.networks -->
<!-- ## services.SERVICE.mac_address -->
<!-- ## services.SERVICE.mem_limit [DEPRECATED] -->
<!-- ## services.SERVICE.mem_reservation [DEPRECATED] -->
<!-- ## services.SERVICE.mem_swappiness -->
<!-- ## services.SERVICE.memswap_limit -->
<!-- ## services.SERVICE.oom_kill_disable -->
<!-- ## services.SERVICE.oom_score_adj -->
<!-- ## services.SERVICE.pid -->
<!-- ## services.SERVICE.pid_limit -->
<!-- ## services.SERVICE.platform [???] -->
<!-- ## services.SERVICE.ports -->
<!-- ## services.SERVICE.privileged -->
<!-- ## services.SERVICE.pull_policy [???] -->
<!-- ## services.SERVICE.read_only -->
<!-- ## services.SERVICE.restart -->
<!-- ## services.SERVICE.runtime [DEPRECATED] -->
<!-- ## services.SERVICE.secrets -->
<!-- ## services.SERVICE.security_opt [???] -->
<!-- ## services.SERVICE.shm_size -->
<!-- ## services.SERVICE.stdin_open -->
<!-- ## services.SERVICE.stop_grace_period -->
<!-- ## services.SERVICE.stop_signal -->
<!-- ## services.SERVICE.sysctls -->
<!-- ## services.SERVICE.tmpfs -->
<!-- ## services.SERVICE.tty -->
<!-- ## services.SERVICE.ulimits -->
<!-- ## services.SERVICE.userns_mode -->
<!-- ## services.SERVICE.volumes[i].type volume -->
<!-- ## services.SERVICE.volumes[i].type tmpfs -->
<!-- ## services.SERVICE.volumes[i].type npipe -->
<!-- ## services.SERVICE.volumes[i].bind -->
<!-- ## services.SERVICE.volumes[i].volume -->
<!-- ## services.SERVICE.volumes[i].tmpfs -->
<!-- ## services.SERVICE.volumes[i].consistency -->
<!-- ## services.SERVICE.volumes_from -->
<!-- ## version [DEPRECATED] -->
<!-- ## volumes -->
