package me.adarlan.dockerflow;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobModel {

    private String name;

    private String status;

    private List<Map<String, String>> rules;

    private List<String> allDependencies;

    private List<String> directDependencies;

    private Integer dependencyLevel;
}