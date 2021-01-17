package me.adarlan.plankton.workflow;

public class ServiceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ServiceNotFoundException(String serviceName) {
        super("Service not found: " + serviceName);
    }
}
