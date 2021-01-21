package me.adarlan.plankton.pipeline.dependencies;

import java.io.IOException;
import java.net.Socket;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import me.adarlan.plankton.pipeline.Job;
import me.adarlan.plankton.pipeline.JobDependency;
import me.adarlan.plankton.pipeline.JobStatus;

@EqualsAndHashCode
@ToString(of = "port")
public class WaitPort implements JobDependency {

    Job parentJob;

    Job requiredJob;

    Integer port;

    public WaitPort(Job parentJob, Job requiredJob, Integer port) {
        this.parentJob = parentJob;
        this.requiredJob = requiredJob;
        this.port = port;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public boolean isSatisfied() {
        if (requiredJob.status().isRunning()) {
            try (Socket s = new Socket("localhost", port)) {
                return true;
            } catch (IOException ex) {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isBlocked() {
        JobStatus status = requiredJob.status();
        return !(status.isWaiting() || status.isRunning());
    }

    @Override
    public Job job() {
        return requiredJob;
    }
}
