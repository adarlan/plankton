package me.adarlan.plankton.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import me.adarlan.plankton.core.Pipeline;
import me.adarlan.plankton.dto.PipelineDto;

@RestController
@CrossOrigin(origins = "*")
public class RestService {

    @Autowired
    private Pipeline pipeline;

    @GetMapping("/hello")
    public String hello() {
        return "Hello";
    }

    @GetMapping("/pipeline")
    public PipelineDto pipline() {
        return new PipelineDto(pipeline);
    }
}
