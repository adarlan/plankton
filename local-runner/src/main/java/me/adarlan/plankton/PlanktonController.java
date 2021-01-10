package me.adarlan.plankton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.adarlan.plankton.api.Pipeline;

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

    // @Autowired
    // PlanktonSerializer planktonSerializer;

    // @GetMapping("/jobs")
    // public List<SerializableJob> getJobs() {
    // return planktonSerializer.getSerializableJobs();
    // }

    // @GetMapping("/jobs")
    // public Set<Job.Data> getJobs() {
    // return pipeline.getData().jobs;
    // }

    // @GetMapping("/jobs/{name}")
    // public Job.Data getJobByName(@PathVariable String name) {
    // return pipeline.getJobByName(name).getData();
    // }

    // @GetMapping("/jobs/{name}/cancel")
    // public Job.Data cancelJob(@PathVariable String name) {
    // Job job = pipeline.getJobByName(name);
    // jobScheduler.cancel(job);
    // while (true) {
    // if (job.finalStatus != null) {
    // return job.getData();
    // }
    // }
    // }

}
