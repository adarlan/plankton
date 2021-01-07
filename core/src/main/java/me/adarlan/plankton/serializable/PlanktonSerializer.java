package me.adarlan.plankton.serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import me.adarlan.plankton.Job;
import me.adarlan.plankton.Pipeline;

public class PlanktonSerializer {

    private final Pipeline pipeline;

    private final Map<String, SerializableJob> serializableJobsByName = new HashMap<>();
    private final List<SerializableJob> serializableJobs = new ArrayList<>();

    public PlanktonSerializer(Pipeline pipeline) {
        this.pipeline = pipeline;
        pipeline.getJobs().forEach(job -> {
            SerializableJob serializableJob = SerializableJob.of(job);
            serializableJobs.add(serializableJob);
            serializableJobsByName.put(serializableJob.name, serializableJob);
            // initializeJobDependencies(serializableJob);
        });
    }

    public List<SerializableJob> getSerializableJobs() {
        return Collections.unmodifiableList(serializableJobs);
    }

    // private void initializeJobDependencies(SerializableJob serializableJob) {
    //     initializeJobDependencies(serializableJob, new HashSet<>());
    // }

    // private int initializeJobDependencies(SerializableJob serializableJob, Set<SerializableJob> knownDependents) {
    //     //if (job.allDependents == null) {
    //     //    job.allDependents = new HashSet<>();
    //     //}
    //     // knownDependents.forEach(kd -> job.allDependents.add(kd));
    //     // job.allDependencies = new HashSet<>();
    //     // job.directDependencies = new HashSet<>();
    //     serializableJob.dependencyLevel = null;
    //     if (serializableJob.dependencyLevel == null) {
    //         knownDependents.add(serializableJob);
    //         int maxDepth = -1;
    //         for (Rule rule : serializableJob.rules) {
    //             if (rule instanceof RuleWithDependency) {
    //                 Job dependency = ((RuleWithDependency) rule).getRequiredJob();
    //                 if (knownDependents.contains(dependency)) {
    //                     throw new PlanktonException("Dependency loop");
    //                 }
    //                 int d = initializeJobDependencies(dependency, knownDependents);
    //                 if (d > maxDepth)
    //                     maxDepth = d;
    //                 // job.allDependencies.add(dependency);
    //                 // job.allDependencies.addAll(dependency.allDependencies);
    //                 // job.directDependencies.add(dependency);
    //             }
    //         }
    //         serializableJob.dependencyLevel = maxDepth + 1;
    //     }
    //     return serializableJob.dependencyLevel;
    // }

}
