package me.adarlan.plankton.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Data;
import me.adarlan.plankton.core.Pipeline;

@Data
public class PipelineDto {

    List<StageDto> stages = new ArrayList<>();
    // TODO rename? -> dependencyLevels

    public PipelineDto(Pipeline pipeline) {
        pipeline.jobs().forEach(job -> {
            int stageIndex = job.dependencyLevel();
            for (int i = this.stages.size(); i <= stageIndex; i++) {
                this.stages.add(new StageDto());
            }
            StageDto stageDto = this.stages.get(stageIndex);
            stageDto.jobs.add(new JobDto(job));
        });
        this.stages.forEach(this::sortJobsByName);
    }

    private void sortJobsByName(StageDto stageDto) {
        Collections.sort(stageDto.jobs, (arg0, arg1) -> arg0.name.compareTo(arg1.name));
    }
}
