package me.adarlan.plankton.pipeline;

public class ServiceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ServiceNotFoundException(String serviceName) {
        super("Service not found: " + serviceName);
    }
}
