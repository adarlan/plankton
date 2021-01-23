package me.adarlan.plankton.dto;

import java.util.Collections;

import me.adarlan.plankton.pipeline.Job;
import me.adarlan.plankton.pipeline.Pipeline;

public class DtoService {

    public DtoService() {
        super();
    }

    public PipelineDto dtoOf(Pipeline pipeline) {
        PipelineDto pipelineDto = new PipelineDto();
        pipelineDto.id = pipeline.id();
        pipeline.jobs().forEach(job -> {
            int stageIndex = job.dependencyLevel();
            for (int i = pipelineDto.stages.size(); i <= stageIndex; i++) {
                pipelineDto.stages.add(new StageDto());
            }
            StageDto stageDto = pipelineDto.stages.get(stageIndex);
            stageDto.jobs.add(dtoOf(job));
        });
        pipelineDto.stages.forEach(this::sortJobsByName);
        return pipelineDto;
    }

    public JobDto dtoOf(Job job) {
        JobDto jobDto = new JobDto();
        jobDto.name = job.name();
        jobDto.status = job.status().toString().toLowerCase();
        job.dependencies().forEach(dependency -> {
            DependencyDto dependencyDto = new DependencyDto();
            dependencyDto.description = dependency.toString();
            jobDto.dependencies.add(dependencyDto);
        });
        sortDependenciesByName(jobDto);
        return jobDto;
    }

    private void sortJobsByName(StageDto stageDto) {
        Collections.sort(stageDto.jobs, (arg0, arg1) -> arg0.name.compareTo(arg1.name));
    }

    private void sortDependenciesByName(JobDto jobDto) {
        Collections.sort(jobDto.dependencies, (arg0, arg1) -> arg0.description.compareTo(arg1.description));
    }
}