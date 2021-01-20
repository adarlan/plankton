package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import me.adarlan.plankton.dto.DtoService;
import me.adarlan.plankton.dto.PipelineDto;
import me.adarlan.plankton.pipeline.Pipeline;

@RestController
@CrossOrigin(origins = "*")
public class DtoRest {

    @Autowired
    private Pipeline pipeline;

    private DtoService dtoService = new DtoService();

    @GetMapping("/pipeline")
    public PipelineDto pipline() {
        return dtoService.dtoOf(pipeline);
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello";
    }
}
