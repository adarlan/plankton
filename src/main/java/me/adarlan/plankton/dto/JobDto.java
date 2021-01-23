package me.adarlan.plankton.dto;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class JobDto {

    String name;
    String status;
    Map<String, String> dependencies = new HashMap<>();
}
