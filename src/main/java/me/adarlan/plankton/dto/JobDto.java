package me.adarlan.plankton.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class JobDto {

    String name;
    String status;
    List<DependencyDto> dependencies = new ArrayList<>();
}
