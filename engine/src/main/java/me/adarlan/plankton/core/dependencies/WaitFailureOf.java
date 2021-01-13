package me.adarlan.plankton.core.dependencies;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.plankton.core.Service;
import me.adarlan.plankton.core.ServiceDependency;
import me.adarlan.plankton.core.ServiceDependencyStatus;
import me.adarlan.plankton.core.ServiceStatus;

@EqualsAndHashCode(of = { "parentService", "requiredService" })
public class WaitFailureOf implements ServiceDependency {

    @Getter
    Service parentService;

    @Getter
    Service requiredService;

    @Getter
    ServiceDependencyStatus status = ServiceDependencyStatus.WAITING;

    public WaitFailureOf(Service parentService, Service requiredService) {
        this.parentService = parentService;
        this.requiredService = requiredService;
    }

    @Override
    public Boolean updateStatus() {
        if (status.equals(ServiceDependencyStatus.WAITING) && requiredService.hasEnded()) {
            if (requiredService.getStatus().equals(ServiceStatus.FAILURE)) {
                status = ServiceDependencyStatus.PASSED;
                return true;
            } else {
                status = ServiceDependencyStatus.BLOCKED;
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        switch (status) {
            case WAITING:
                return parentService.getName() + " -> Waiting for " + requiredService.getName() + " failure";
            case PASSED:
                return parentService.getName() + " -> Satisfied because " + requiredService.getName() + " failed";
            case BLOCKED:
                return parentService.getName() + " -> Blocked because " + requiredService.getName() + " succeeded";
            default:
                return super.toString();
        }
    }
}
