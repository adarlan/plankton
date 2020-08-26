package me.adarlan.dockerflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class RestApi {

    @Autowired
    private Pipeline pipeline;

    @GetMapping("/jobs")
    public List<Map<String, Object>> getJobs() {
        List<Map<String, Object>> jobs = new ArrayList<>();
        pipeline.getJobs().forEach(job -> jobs.add(job.toMap()));
        return jobs;
    }

}