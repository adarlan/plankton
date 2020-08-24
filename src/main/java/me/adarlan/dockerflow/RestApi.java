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
    public List<JobModel> getJobs() {
        List<JobModel> jobs = new ArrayList<>();
        pipeline.getJobs().forEach(job -> {

            JobModel jobModel = new JobModel();
            jobModel.setName(job.getName());
            jobModel.setStatus(job.getStatus().toString().toLowerCase());

            List<Map<String, String>> rules = new ArrayList<>();
            job.getRules().forEach(rule -> {
                Map<String, String> r = new HashMap<>();
                r.put("name", rule.getName().substring(11));
                r.put("value", rule.getValue());
                r.put("status", rule.getRuleStatus().toString().toLowerCase());
                rules.add(r);
            });
            jobModel.setRules(rules);

            List<String> allDependencies = new ArrayList<>();
            List<String> directDependencies = new ArrayList<>();
            job.getAllDependencies().forEach(d -> allDependencies.add(d.getName()));
            job.getDirectDependencies().forEach(d -> directDependencies.add(d.getName()));
            jobModel.setAllDependencies(allDependencies);
            jobModel.setDirectDependencies(directDependencies);
            jobModel.setDependencyLevel(job.getDependencyLevel());

            jobs.add(jobModel);
        });
        return jobs;
    }

}