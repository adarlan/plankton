package me.adarlan.plankton;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.adarlan.plankton.serializable.SerializableJob;
import me.adarlan.plankton.serializable.PlanktonSerializer;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class PlanktonController {

    @Autowired
    PlanktonSerializer planktonSerializer;

    @GetMapping("/jobs")
    public List<SerializableJob> getJobs() {
        return planktonSerializer.getSerializableJobs();
    }

    /*
     * @GetMapping("/jobs") public Set<Job.Data> getJobs() { return
     * pipeline.getData().jobs; }
     * 
     * @GetMapping("/jobs/{name}") public Job.Data getJobByName(@PathVariable String
     * name) { return pipeline.getJobByName(name).getData(); }
     * 
     * @GetMapping("/jobs/{name}/cancel") public Job.Data cancelJob(@PathVariable
     * String name) { Job job = pipeline.getJobByName(name);
     * jobScheduler.cancel(job); while (true) { if (job.finalStatus != null) {
     * return job.getData(); } } }
     */
}
