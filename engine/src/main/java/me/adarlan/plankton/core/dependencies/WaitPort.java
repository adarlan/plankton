package me.adarlan.plankton.core.dependencies;

import java.io.IOException;
import java.net.Socket;

import lombok.EqualsAndHashCode;
import lombok.Getter;
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
                return parentService.getName() + " -> Waiting for port " + port + ", which is " + providedBy;
            case PASSED:
                return parentService.getName() + " -> Satisfied because port " + port + ", " + providedBy
                        + ", is responding";
            case BLOCKED:
                return parentService.getName() + " -> Blocked because " + requiredService.getName()
                        + ", which provides port " + port + ", is not running anymore";
            default:
                return super.toString();
        }
    }
}
