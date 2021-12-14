package plankton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import plankton.dto.PipelineDto;
import plankton.pipeline.Pipeline;

@RestController
@CrossOrigin(origins = "*")
public class PlanktonApi {

    @Autowired
    private PlanktonSetup planktonSetup;

    @GetMapping("/pipeline")
    public PipelineDto pipline() {
        Pipeline pipeline = planktonSetup.getPipeline();
        return new PipelineDto(pipeline);
    }
}
