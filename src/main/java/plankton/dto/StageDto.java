package plankton.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class StageDto {
    // TODO -> DependencyLevelDto

    Integer index;
    List<JobDto> jobs = new ArrayList<>();
}
