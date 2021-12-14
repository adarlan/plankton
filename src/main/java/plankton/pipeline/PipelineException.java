package plankton.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(PipelineException.class);

    PipelineException(String msg) {
        super(msg);
        logger.error(msg);
    }

    PipelineException(String msg, Throwable e) {
        super(msg, e);
        logger.error("{}", msg, e);
    }

    PipelineException(Job job, String msg) {
        super(job.name + ": " + msg);
        logger.error("{}{}", job.logPrefix, msg);
    }

    PipelineException(Job job, String msg, Throwable e) {
        super(job.name + ": " + msg, e);
        logger.error("{}{}", job.logPrefix, msg, e);
    }

    PipelineException(JobInstance instance, String msg, Throwable e) {
        super(instance.job.name + "[" + instance.index + "]" + ": " + msg, e);
        logger.error("{} -> {}", instance, msg, e);
    }
}
