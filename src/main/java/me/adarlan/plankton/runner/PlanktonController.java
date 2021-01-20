package me.adarlan.plankton.runner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.adarlan.plankton.pipeline.Pipeline;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class PlanktonController {

    @Autowired
    private Pipeline pipeline;

    @GetMapping("/pipeline-id")
    public String getPipelineId() {
        return pipeline.getId();
    }
}
