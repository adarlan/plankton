package me.adarlan.dockerflow;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DockerflowRunner implements CommandLineRunner {

    @Autowired
    private Pipeline pipeline;

    @Autowired
    private Docker docker;

    @Autowired
    private DockerCompose dockerCompose;

    private boolean thereAreJobsToSchedule = true;
    private boolean thereAreJobsToRun = true;

    private Set<Thread> startJob_threads = new HashSet<>();
    private Set<Thread> runContainer_threads = new HashSet<>();
    private Set<Thread> followContainer_threads = new HashSet<>();

    @Override
    public void run(String... args) throws Exception {
        Logger.trace(() -> "DockerflowRunner.run(" + args + ")");
        setDefaultUncaughtExceptionHandler();
        new Thread(this::scheduleJobs).start();
        new Thread(this::startJobs).start();
    }

    private void setDefaultUncaughtExceptionHandler() {
        Logger.trace(() -> "DockerflowRunner.setDefaultUncaughtExceptionHandler()");
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            if (exception instanceof JobFailedException) {
                final JobFailedException jobFailedException = (JobFailedException) exception;
                final String jobName = jobFailedException.getJobName();
                synchronized (pipeline) {
                    final Job job = pipeline.getJobByName(jobName);
                    job.state.set(JobStatus.FAILED);
                }
            } else {
                throw new DockerflowException(thread.getName(), exception);
            }
            // TODO é preciso parar todas as threads?
        });
    }

    private void scheduleJobs() {
        Logger.trace(() -> "DockerflowRunner.scheduleJobs()");
        while (thereAreJobsToSchedule) {
            Set<Job> waitingJobs;
            synchronized (pipeline) {
                waitingJobs = pipeline.getJobsByStatus(JobStatus.WAITING);
            }
            Logger.debug(() -> "waitingJobs = " + waitingJobs);
            if (waitingJobs.isEmpty()) {
                thereAreJobsToSchedule = false;
                Logger.info(() -> "No more jobs to schedule");
            }
            waitingJobs.forEach(this::scheduleJob);
        }
    }

    private void scheduleJob(Job job) {
        Logger.trace(() -> "DockerflowRunner.scheduleJob(" + job.name + ")");
        synchronized (pipeline) {
            boolean passed = true;
            boolean blocked = false;
            for (final Rule rule : job.getRules()) {
                if (rule.updateStatus()) {
                    Logger.info(
                            () -> job.name + "." + rule.getName() + "(" + rule.getValue() + "): " + rule.getStatus());
                }
                if (!rule.getStatus().equals(RuleStatus.PASSED))
                    passed = false;
                if (rule.getStatus().equals(RuleStatus.BLOCKED))
                    blocked = true;
            }
            if (passed) {
                job.state.set(JobStatus.SCHEDULED);
            } else if (blocked) {
                job.state.set(JobStatus.BLOCKED);
            }
        }
    }

    private void startJobs() {
        Logger.trace(() -> "DockerflowRunner.startJobs()");
        while (thereAreJobsToRun) {
            Set<Job> scheduledJobs;
            synchronized (pipeline) {
                scheduledJobs = pipeline.getJobsByStatus(JobStatus.SCHEDULED);
            }
            Logger.debug(() -> "scheduledJobs = " + scheduledJobs);
            if (!thereAreJobsToSchedule && scheduledJobs.isEmpty()) {
                thereAreJobsToRun = false;
                Logger.info(() -> "No more jobs to start");
            }
            scheduledJobs.forEach(this::startJob);
        }
    }

    private void startJob(Job job) {
        Logger.trace(() -> "DockerflowRunner.startJob(" + job.name + ")");
        synchronized (pipeline) {
            job.state.set(JobStatus.RUNNING);
        }
        Thread thread = new Thread(() -> {
            createContainers(job);
            runAndFollowContainers(job);
        });
        startJob_threads.add(thread);
        thread.start();
    }

    private void createContainers(Job job) {
        Logger.trace(() -> "DockerflowRunner.createContainers(" + job.name + ")");
        try {
            if (!dockerCompose.createContainers(job)) {
                throw new JobFailedException(job.name, "Job failed when creating containers");
            }
        } catch (InterruptedException e) {
            Logger.error(() -> "Interruption when creating containers for: " + job.name);
            Thread.currentThread().interrupt();
            throw new DockerflowException("Interruption when creating containers for '" + job.name + "'", e);
        }
    }

    private void runAndFollowContainers(Job job) {
        Logger.trace(() -> "DockerflowRunner.runAndFollowContainers(" + job.name + ")");
        for (int i = 1; i <= job.scale; i++) {
            final String containerName = dockerCompose.projectName + "_" + job.name + "_" + i;
            runContainerThread(job, containerName);
            followContainerThread(job, containerName);
        }
    }

    private void runContainerThread(Job job, String containerName) {
        Logger.trace(() -> "DockerflowRunner.runContainerThread(" + job.name + ", " + containerName + ")");
        Thread thread = new Thread(() -> runContainer(job, containerName));
        runContainer_threads.add(thread);
        thread.start();
    }

    private void followContainerThread(Job job, String containerName) {
        Logger.trace(() -> "DockerflowRunner.followContainerThread(" + job.name + ", " + containerName + ")");
        Thread thread = new Thread(() -> followContainer(job, containerName));
        followContainer_threads.add(thread);
        thread.start();
    }

    private void runContainer(Job job, String containerName) {
        Logger.trace(() -> "DockerflowRunner.runContainer(" + job.name + ", " + containerName + ")");
        try {
            if (!docker.runContainer(job, containerName)) {
                throw new JobFailedException(job.name, "Job failed when running container: " + containerName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DockerflowException("Interruption when running container '" + containerName + "'", e);
        }
    }

    private void followContainer(Job job, String containerName) {
        Logger.trace(() -> "DockerflowRunner.followContainer(" + job.name + ", " + containerName + ")");
        boolean exited = false;
        while (!exited) {
            ContainerState containerState = getContainerState(containerName);
            Logger.debug(() -> containerName + ".state = " + containerState);
            if (containerState.status.equals("exited")) {
                exited = true;
                synchronized (pipeline) {
                    job.state.increaseExitedCount();
                    Logger.debug(() -> containerName + ".exitedCount = " + job.state.getExitedCount());
                    if (containerState.exitCode == 0 && job.state.getFinalStatus() == null
                            && job.state.getExitedCount() == job.scale) {
                        job.state.set(JobStatus.FINISHED);
                    } else if (containerState.exitCode != 0 && job.state.getFinalStatus() == null) {
                        job.state.set(JobStatus.FAILED);
                    }
                }
            }
        }
    }

    private ContainerState getContainerState(String containerName) {
        Logger.trace(() -> "DockerflowRunner.getContainerState(" + containerName + ")");
        try {
            Thread.sleep(1000);
            return docker.containerState(containerName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DockerflowException("Interruption when getting state of container '" + containerName + "'", e);
        }
    }
}