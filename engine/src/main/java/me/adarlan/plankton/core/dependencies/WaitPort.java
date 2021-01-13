package me.adarlan.plankton.core.dependencies;

import java.io.IOException;
import java.net.Socket;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.plankton.core.Logger;
import me.adarlan.plankton.core.Service;
import me.adarlan.plankton.core.ServiceDependency;
import me.adarlan.plankton.core.ServiceDependencyStatus;
import me.adarlan.plankton.core.ServiceStatus;

@EqualsAndHashCode(of = { "parentService", "requiredService", "port" })
public class WaitPort implements ServiceDependency {

    @Getter
    Service parentService;

    @Getter
    Service requiredService;

    @Getter
    Integer port;

    @Getter
    ServiceDependencyStatus status = ServiceDependencyStatus.WAITING;

    private final Logger logger = Logger.getLogger();

    public WaitPort(Service parentService, Service requiredService, Integer port) {
        this.parentService = parentService;
        this.requiredService = requiredService;
        this.port = port;
    }

    @Override
    public Boolean updateStatus() {
        if (status.equals(ServiceDependencyStatus.WAITING)) {
            if (requiredService.hasEnded()) {
                status = ServiceDependencyStatus.BLOCKED;
                return true;
            } else if (requiredService.getStatus().equals(ServiceStatus.RUNNING)) {
                try (Socket s = new Socket("localhost", port)) {
                    logger.info(
                            () -> "PORT " + port + "; isConnected: " + s.isConnected() + "; isBound: " + s.isBound());
                    status = ServiceDependencyStatus.PASSED;
                    return true;
                } catch (IOException ex) {
                    /* ignore */
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        String providedBy = "provided by " + requiredService.getName();
        switch (status) {
            case WAITING:
                return "Waiting for port " + port + ", which is " + providedBy;
            case PASSED:
                return "Satisfied because port " + port + ", " + providedBy + ", is responding";
            case BLOCKED:
                return "Blocked because " + requiredService.getName() + ", which provides port " + port
                        + ", is not running anymore";
            default:
                return super.toString();
        }
    }
}
