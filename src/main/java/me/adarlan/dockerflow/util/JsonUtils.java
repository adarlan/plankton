package me.adarlan.dockerflow.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.adarlan.dockerflow.DockerflowException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JsonUtils {

    public static List<Map<String, Object>> parseObjectArray(String json) {
        List<Map<String, Object>> result;
        try {
            result = new ObjectMapper().readValue(json, ArrayList.class);
        } catch (JsonProcessingException e) {
            throw new DockerflowException("Unable to parse 'docker inspect' JSON", e);
        }
        return result;
    }
}