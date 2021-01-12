package me.adarlan.plankton.core.dependencies;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.adarlan.plankton.core.Service;
import me.adarlan.plankton.core.ServiceDependency;
import me.adarlan.plankton.core.ServiceDependencyStatus;
import me.adarlan.plankton.core.ServiceStatus;

@EqualsAndHashCode(of = { "parentService", "requiredService" })
public class WaitSuccessOf implements ServiceDependency {

    @Getter
    Service parentService;

    @Getter
    Service requiredService;

    @Getter
    ServiceDependencyStatus status = ServiceDependencyStatus.WAITING;

    public WaitSuccessOf(Service parentService, Service requiredService) {
        this.parentService = parentService;
        this.requiredService = requiredService;
    }

    @Override
    public Boolean updateStatus() {
        if (status.equals(ServiceDependencyStatus.WAITING) && requiredService.hasEnded()) {
            if (requiredService.getStatus().equals(ServiceStatus.SUCCESS)) {
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
        return parentService.getName() + "." + getClass().getSimpleName() + "(" + requiredService.getName() + ") -> " + status;
    }
}
