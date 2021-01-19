package me.adarlan.plankton.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import me.adarlan.plankton.workflow.Pipeline;

@RestController
// @ConditionalOnProperty("plankton.web.enable")
@CrossOrigin(origins = "*")
@RequestMapping("/api")
public class PlanktonController {

    @Autowired
    private Pipeline pipeline;

    @GetMapping("/pipeline-id")
    public String getPipelineId() {
        return pipeline.getId();
    }

    // @GetMapping("/services")
    // public List<Service> getServices() {
    // }

    // @GetMapping("/services")
    // public List<Service> getServices() {
    // }

    // @GetMapping("/services/{name}")
    // public Service getServiceByName(@PathVariable String name) {
    // }

    // @GetMapping("/services/{name}/cancel")
    // public Service cancelService(@PathVariable String name) {
    // Service service = pipeline.getServiceByName(name);
    // serviceScheduler.cancel(service);
    // while (true) {
    // if (service.finalStatus != null) {
    // return service.getData();
    // }
    // }
    // }

}
