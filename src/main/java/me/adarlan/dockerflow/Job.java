package me.adarlan.dockerflow;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.dockerflow.rules.Rule;

@EqualsAndHashCode(of = "name")
public class Job {

    @Getter
    private final String name;

    @Getter
    Set<Rule> rules;

    @Getter
    Set<Job> allDependencies;

    @Getter
    Set<Job> directDependencies;

    @Getter
    Integer dependencyLevel;

    @Getter
    Set<Job> allDependents = new HashSet<>();

    @Getter
    private JobStatus status = JobStatus.WAITING;

    @Getter
    private JobStatus finalStatus = null;

    @Getter
    private Instant initialInstant = null;

    @Getter
    private Instant finalInstant = null;

    @Getter
    Integer exitCode = null;

    /*
     * TODO Tirar daqui todos os atributos não serializáveis Colocá-los em services?
     * Esta classe tem que ser gravada direto no banco e o runner não se comunica
     * com a API
     */

    final Map<String, Object> data;

    Process process = null;

    Job(final String name, final Map<String, Object> data) {
        this.name = name;
        this.data = data;
    }

    void setStatus(final JobStatus status) {
        this.status = status;
        switch (status) {
            case RUNNING:
                initialInstant = Instant.now();
                break;
            case INTERRUPTED:
            case CANCELED:
            case FAILED:
            case TIMEOUT:
            case FINISHED:
                finalInstant = Instant.now();
                finalStatus = status;
                break;
            default:
                break;
        }
        System.out.println(this);
    }

    @Override
    public String toString() {
        return name + ": " + status.toString().toLowerCase();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("name", name);
        map.put("status", status.toString().toLowerCase());
        map.put("finalStatus", finalStatus == null ? null : finalStatus.toString().toLowerCase());
        map.put("initialInstant", initialInstant);
        map.put("finalInstant", finalInstant);
        map.put("exitCode", exitCode);

        List<Map<String, String>> rs = new ArrayList<>();
        rules.forEach(rule -> {
            Map<String, String> r = new HashMap<>();
            r.put("name", rule.getName().substring(11));
            r.put("value", rule.getValue());
            r.put("status", rule.getRuleStatus().toString().toLowerCase());
            rs.add(r);
        });
        map.put("rules", rs);

        List<String> allDeps = new ArrayList<>();
        List<String> directDeps = new ArrayList<>();
        List<String> depdts = new ArrayList<>();
        allDependencies.forEach(d -> allDeps.add(name));
        directDependencies.forEach(d -> directDeps.add(name));
        allDependents.forEach(d -> depdts.add(name));
        map.put("allDependents", depdts);
        map.put("allDependencies", allDeps);
        map.put("directDependencies", directDeps);
        map.put("dependencyLevel", dependencyLevel);

        return map;
    }
}