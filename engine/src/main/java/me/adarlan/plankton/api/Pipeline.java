package me.adarlan.plankton.api;

import java.util.Set;

public interface Pipeline {

    String getId();

    Job getJobByName(String name);

    Set<Job> getJobs();

    void run() throws InterruptedException;
}
