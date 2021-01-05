package me.adarlan.dockerflow.data;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import me.adarlan.dockerflow.Pipeline;

@RequiredArgsConstructor
public class DockerflowData {

    private final Pipeline pipeline;

    public List<Job> getJobs() {
        return new ArrayList<>();
    }
}
