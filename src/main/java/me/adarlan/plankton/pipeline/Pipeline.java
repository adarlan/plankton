package me.adarlan.plankton.pipeline;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.adarlan.plankton.bash.BashScript;
import me.adarlan.plankton.bash.BashScriptFailedException;
import me.adarlan.plankton.compose.ComposeAdapter;
import me.adarlan.plankton.compose.ComposeDocument;
import me.adarlan.plankton.pipeline.dependencies.WaitFailureOf;
import me.adarlan.plankton.pipeline.dependencies.WaitPort;
import me.adarlan.plankton.pipeline.dependencies.WaitSuccessOf;

@EqualsAndHashCode(of = "id")
@ToString(of = "id")
public class Pipeline {

    private final ComposeDocument composeDocument;
    final ComposeAdapter composeAdapter;

    private final String id;

    private final Set<Job> jobs = new HashSet<>();
    private final Map<String, Job> jobsByName = new HashMap<>();
    private final Map<Job, Map<String, String>> labelsByJobAndName = new HashMap<>();
    private final Map<Integer, Job> externalPorts = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    Integer biggestJobNameLength;
    private static final String LOADING = "Loading " + Pipeline.class.getSimpleName() + " ... ";

    public Pipeline(PipelineConfiguration configuration) {

        logger.info(LOADING);

        this.composeAdapter = configuration.composeAdapter();
        this.composeDocument = configuration.composeDocument();
        this.id = composeDocument.projectName();

        logger.info("{}id={}", LOADING, id);
        logger.info("{}composeDocument={}", LOADING, composeDocument);
        logger.info("{}composeAdapter={}", LOADING, composeAdapter);

        instantiateJobs();
        jobs.forEach(this::initializeJobLabels);
        jobs.forEach(this::initializeJobExpression);
        jobs.forEach(this::initializeJobScaleAndInstances);
        jobs.forEach(this::initializeJobTimeout);
        jobs.forEach(this::initializeExternalPorts);
        jobs.forEach(this::initializeJobDependencies);
        jobs.forEach(this::initializeJobStatus);
        jobs.forEach(this::initializeJobDependencyLevel);
        this.initializeInstanceNamesAndBiggestName();
        this.initializeColors();
    }

    private void instantiateJobs() {
        logger.trace("{}Instantiating jobs", LOADING);
        composeDocument.serviceNames().forEach(name -> {
            Job job = new Job(this, name);
            this.jobs.add(job);
            this.jobsByName.put(name, job);
        });
        logger.info("{}jobs={}", LOADING, jobs);
    }

    private void initializeJobLabels(Job job) {
        logger.trace("{}Initializing {}.labels", LOADING, job.name);
        Map<String, String> labelsByName = composeDocument.labelsMapOf(job.name);
        labelsByJobAndName.put(job, labelsByName);
        logger.info("{}{}.labels={}", LOADING, job.name, labelsByName);
    }

    private void initializeJobExpression(Job job) {
        logger.trace("{}Initializing {}.expression", LOADING, job.name);
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        String labelName = "plankton.enable.if";
        if (labelsByName.containsKey(labelName)) {
            job.expression = labelsByName.get(labelName);
        }
        logger.info("{}{}.expression={}", LOADING, job.name, job.expression);
    }

    private void initializeJobScaleAndInstances(Job job) {
        logger.trace("{}Initializing {}.scale", LOADING, job.name);
        int scale = 1; // TODO read from compose document
        job.scale = scale;
        logger.info("{}{}.scale={}", LOADING, job.name, job.scale);

        logger.trace("{}Initializing {}.instances", LOADING, job.name);
        for (int instanceNumber = 1; instanceNumber <= scale; instanceNumber++) {
            JobInstance instance = new JobInstance(job, instanceNumber);
            job.instances.add(instance);
        }
        logger.info("{}{}.instances={}", LOADING, job.name, job.instances);
    }

    private void initializeJobTimeout(Job job) {
        logger.trace("{}Initializing {}.timeoutLimit", LOADING, job.name);
        Map<String, String> labelsByName = labelsByJobAndName.computeIfAbsent(job, j -> new HashMap<>());
        String labelName = "plankton.timeout";
        if (labelsByName.containsKey(labelName)) {
            String labelValue = labelsByName.get(labelName);
            job.timeoutLimit = Duration.of(Long.parseLong(labelValue), ChronoUnit.MINUTES);
        } else {
            job.timeoutLimit = Duration.of(1L, ChronoUnit.MINUTES);
        }
        logger.info("{}{}.timeoutLimit={}", LOADING, job.name, job.timeoutLimit);
    }

    private void initializeExternalPorts(Job job) {
        List<Map<String, Object>> ports = composeDocument.servicePortsOf(job.name);
        ports.forEach(p -> {
            Integer externalPort = (Integer) p.get("published"); // TODO what if published is null?
            externalPorts.put(externalPort, job);
        });
    }

    private void initializeJobDependencies(Job job) {
        logger.trace("{}Initializing {}.dependencies", LOADING, job.name);
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        labelsByName.forEach((labelName, labelValue) -> {

            if (Utils.stringMatchesRegex(labelName, "^plankton\\.wait\\.success\\.of$")) {
                String requiredJobName = labelValue;
                Job requiredJob = this.getJobByName(requiredJobName);
                WaitSuccessOf dependency = new WaitSuccessOf(job, requiredJob);
                job.dependencies.add(dependency);
            }

            if (Utils.stringMatchesRegex(labelName, "^plankton\\.wait\\.failure\\.of$")) {
                String requiredJobName = labelValue;
                Job requiredJob = this.getJobByName(requiredJobName);
                WaitFailureOf dependency = new WaitFailureOf(job, requiredJob);
                job.dependencies.add(dependency);
            }

            else if (Utils.stringMatchesRegex(labelName, "^plankton\\.wait\\.ports$")) {
                Integer port = Integer.parseInt(labelValue);
                Job requiredJob = externalPorts.get(port);
                WaitPort dependency = new WaitPort(job, requiredJob, port);
                job.dependencies.add(dependency);
            }
        });
        logger.info("{}{}.dependencies={}", LOADING, job.name, job.dependencies);
    }

    private void initializeJobStatus(Job job) {
        logger.trace("{}Initializing {}.status", LOADING, job.name);
        if (job.expression != null) {
            evaluateExpression(job);
            if (job.expressionResult) {
                job.status = JobStatus.WAITING;
                // logger.info("{} -> Enabled by expression: {}", job.name, job.expression);
            } else {
                job.status = JobStatus.DISABLED;
                // logger.info("{} -> Disabled by expression: {}", job.name, job.expression);
            }
        } else {
            job.status = JobStatus.WAITING;
        }
        logger.info("{}{}.status={}", LOADING, job.name, job.status);
    }

    private void evaluateExpression(Job job) {
        // TODO do it inside a sandbox container to prevent script injection
        // TODO it needs timeout
        // TODO add variables
        logger.trace("{}Evaluating {}.expression", LOADING, job.name);
        BashScript script = new BashScript();
        script.command(job.expression);
        try {
            script.run();
            job.expressionResult = true;
        } catch (BashScriptFailedException e) {
            job.expressionResult = false;
        }
        logger.info("{}{}.expressionResult={}", LOADING, job.name, job.expressionResult);
    }

    private void initializeInstanceNamesAndBiggestName() {
        biggestJobNameLength = 0;
        for (Job job : enabledJobs()) {
            for (JobInstance instance : job.instances) {
                if (job.scale() == 1) {
                    instance.name = job.name;
                } else {
                    instance.name = job.name + "_" + instance.number;
                }
                int len = instance.name.length();
                if (len > biggestJobNameLength) {
                    biggestJobNameLength = len;
                }
            }
        }
    }

    private void initializeColors() {
        List<String> list = new ArrayList<>();
        list.add(Colors.BRIGHT_BLUE);
        list.add(Colors.BRIGHT_YELLOW);
        // list.add(Colors.BRIGHT_GREEN);
        list.add(Colors.BRIGHT_CYAN);
        list.add(Colors.BRIGHT_PURPLE);
        // list.add(Colors.BRIGHT_RED);
        int jobIndex = 0;
        for (Job job : enabledJobs()) {
            int colorIndex = jobIndex % list.size();
            job.color = list.get(colorIndex);
            jobIndex++;
            job.prefix = Utils.prefixOf(job);
            for (JobInstance instance : job.instances) {
                instance.prefix = Utils.prefixOf(instance);
            }
        }
    }

    private void initializeJobDependencyLevel(Job job) {
        logger.trace("{}Initializing {}.dependencyLevel", LOADING, job.name);
        dependencyLevelOf(job, new HashSet<>());
        logger.info("{}{}.dependencyLevel={}", LOADING, job.name, job.dependencyLevel);
    }

    private int dependencyLevelOf(Job job, Set<Job> knownDependents) {
        if (job.dependencyLevel == null) {
            knownDependents.add(job);
            int maxDepth = -1;
            for (JobDependency dependency : job.dependencies) {
                Job requiredJob = dependency.job();
                if (knownDependents.contains(requiredJob)) {
                    throw new JobDependencyLoopException();
                }
                int d = dependencyLevelOf(requiredJob, knownDependents);
                if (d > maxDepth)
                    maxDepth = d;
            }
            job.dependencyLevel = maxDepth + 1;
        }
        return job.dependencyLevel;
    }

    public void run() throws InterruptedException {
        logger.info("Running {}", this);
        boolean done = false;
        while (!done) {
            done = true;
            for (Job job : waitingOrRunningJobs()) {
                job.refresh();
                if (job.isWaitingOrRunning()) {
                    done = false;
                }
            }
            Thread.sleep(1000);
        }
        logger.info("Finished {}", this);
        composeAdapter.disconnect();
    }

    public String id() {
        return id;
    }

    public Set<Job> jobs() {
        return Collections.unmodifiableSet(jobs);
    }

    public Job getJobByName(String jobName) {
        if (!jobsByName.containsKey(jobName))
            throw new JobNotFoundException(jobName);
        return jobsByName.get(jobName);
    }

    public Set<Job> enabledJobs() {
        return Collections.unmodifiableSet(jobs.stream().filter(Job::isEnabled).collect(Collectors.toSet()));
    }

    public Set<Job> waitingOrRunningJobs() {
        return Collections.unmodifiableSet(jobs.stream().filter(Job::isWaitingOrRunning).collect(Collectors.toSet()));
    }
}
