package me.adarlan.plankton.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class PipelineDto {

    String id;
    List<StageDto> stages = new ArrayList<>();
}
