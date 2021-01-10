package me.adarlan.plankton.core;

import java.util.Set;

public interface Pipeline {

    String getId();

    Job getJobByName(String name);

    Set<Job> getJobs();

    void run() throws InterruptedException;
}
