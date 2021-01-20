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
import lombok.Getter;
import lombok.ToString;
import me.adarlan.plankton.bash.BashScript;
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

    @Getter
    private final String id;

    private final Set<Job> jobs = new HashSet<>();
    private final Map<String, Job> jobsByName = new HashMap<>();
    private final Map<Job, Map<String, String>> labelsByJobAndName = new HashMap<>();
    private final Map<Integer, Job> externalPorts = new HashMap<>();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    Integer biggestJobNameLength;

    public Pipeline(PipelineConfiguration configuration) {
        logger.trace("Instantiate pipeline...");
        this.composeAdapter = configuration.composeAdapter();
        this.composeDocument = configuration.composeDocument();
        this.id = composeDocument.getProjectName();
        instantiateJobs();
        jobs.forEach(this::initializeJobLabels);
        jobs.forEach(this::initializeJobExpression);
        jobs.forEach(this::initializeJobScaleAndInstances);
        jobs.forEach(this::initializeJobTimeout);
        jobs.forEach(this::initializeExternalPorts);
        jobs.forEach(this::initializeJobDependencies);
        jobs.forEach(this::initializeJobStatus);
        this.initializeInstanceNamesAndBiggestName();
        this.initializeColors();
        logger.info("Pipeline id: {}", id);
        logger.trace("Instantiate pipeline... (done)");
    }

    private void instantiateJobs() {
        logger.trace("Instantiate jobs...");
        composeDocument.getServiceNames().forEach(name -> {
            logger.trace("Instantiate job: {}", name);
            Job job = new Job(this, name);
            this.jobs.add(job);
            this.jobsByName.put(name, job);
            logger.trace("Instantiate jobs: {} (done)", name);
        });
        logger.trace("Instantiate jobs... (done)");
    }

    private void initializeJobLabels(Job job) {
        logger.trace("initializeJobLabels: {}", job.name);
        Map<String, String> labelsByName = composeDocument.getServiceLabelsMap(job.name);
        labelsByJobAndName.put(job, labelsByName);
    }

    private void initializeJobExpression(Job job) {
        logger.trace("initializeJobExpression: {}", job.name);
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        String labelName = "plankton.enable.if";
        if (labelsByName.containsKey(labelName)) {
            job.expression = labelsByName.get(labelName);
        }
    }

    private void initializeJobScaleAndInstances(Job job) {
        logger.trace("initializeJobScaleAndInstances: {}", job.name);

        int scale = 1;
        // TODO read from compose document

        job.scale = scale;
        for (int instanceNumber = 1; instanceNumber <= scale; instanceNumber++) {
            JobInstance instance = new JobInstance(job, instanceNumber);
            job.instances.add(instance);
        }
    }

    private void initializeJobTimeout(Job job) {
        logger.trace("initializeJobTimeout: {}", job.name);
        Map<String, String> labelsByName = labelsByJobAndName.get(job);
        String labelName = "plankton.timeout";
        if (labelsByName.containsKey(labelName)) {
            String labelValue = labelsByName.get(labelName);
            job.timeoutLimit = Duration.of(Long.parseLong(labelValue), ChronoUnit.MINUTES);
        } else {
            job.timeoutLimit = Duration.of(1L, ChronoUnit.MINUTES);
        }
    }

    private void initializeExternalPorts(Job job) {
        logger.trace("initializeExternalPorts: {}", job.name);
        List<Map<String, Object>> ports = composeDocument.getServicePorts(job.name);
        ports.forEach(p -> {
            Integer externalPort = (Integer) p.get("published"); // TODO what if published is null?
            externalPorts.put(externalPort, job);
        });
    }

    private void initializeJobDependencies(Job job) {
        logger.trace("initializeJobDependencies: {}", job.name);
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
    }

    private void initializeJobStatus(Job job) {
        logger.trace("initializeJobStatus: {}", job.name);
        if (job.expression != null) {
            evaluateExpression(job);
            if (job.expressionResult) {
                job.status = JobStatus.WAITING;
                logger.info("{} -> Enabled by expression: {}", job.name, job.expression);
            } else {
                job.status = JobStatus.DISABLED;
                logger.info("{} -> Disabled by expression: {}", job.name, job.expression);
            }
        } else {
            job.status = JobStatus.WAITING;
        }
    }

    private void evaluateExpression(Job job) {
        logger.trace("evaluateExpression: {}", job.name);

        final String scriptName = "evaluateExpression_" + job.name;
        BashScript script = new BashScript(scriptName);
        script.command(job.expression);
        script.run();
        // TODO do it inside a sandbox container to prevent script injection
        // TODO it needs timeout
        // TODO add variables

        if (script.getExitCode() == 0) {
            job.expressionResult = true;
        } else {
            job.expressionResult = false;
        }
    }

    private void initializeInstanceNamesAndBiggestName() {
        logger.trace("initializeInstanceNamesAndBiggestName");
        biggestJobNameLength = 0;
        for (Job job : getEnabledJobs()) {
            for (JobInstance instance : job.instances) {
                if (job.getScale() == 1) {
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
        logger.trace("initializeColors");
        List<String> list = new ArrayList<>();
        list.add(Colors.BRIGHT_BLUE);
        list.add(Colors.BRIGHT_YELLOW);
        // list.add(Colors.BRIGHT_GREEN);
        list.add(Colors.BRIGHT_CYAN);
        list.add(Colors.BRIGHT_PURPLE);
        // list.add(Colors.BRIGHT_RED);
        int jobIndex = 0;
        for (Job job : getEnabledJobs()) {
            int colorIndex = jobIndex % list.size();
            job.color = list.get(colorIndex);
            jobIndex++;
            job.prefix = Utils.prefixOf(job);
            for (JobInstance instance : job.instances) {
                instance.prefix = Utils.prefixOf(instance);
            }
        }
    }

    public void run() throws InterruptedException {
        logger.info("Pipeline running");
        boolean done = false;
        while (!done) {
            done = true;
            for (Job job : getWaitingOrRunningJobs()) {
                job.refresh();
                if (job.isWaitingOrRunning()) {
                    done = false;
                }
            }
            Thread.sleep(1000);
        }
        logger.info("Pipeline finished");
    }

    public Set<Job> getJobs() {
        return Collections.unmodifiableSet(jobs);
    }

    public Job getJobByName(String jobName) {
        if (!jobsByName.containsKey(jobName))
            throw new JobNotFoundException(jobName);
        return jobsByName.get(jobName);
    }

    public Set<Job> getEnabledJobs() {
        return Collections.unmodifiableSet(jobs.stream().filter(Job::isEnabled).collect(Collectors.toSet()));
    }

    public Set<Job> getWaitingOrRunningJobs() {
        return Collections.unmodifiableSet(jobs.stream().filter(Job::isWaitingOrRunning).collect(Collectors.toSet()));
    }
}
