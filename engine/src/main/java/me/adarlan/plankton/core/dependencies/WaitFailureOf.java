package me.adarlan.plankton.core.dependencies;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import me.adarlan.plankton.core.Service;
import me.adarlan.plankton.core.ServiceDependency;
import me.adarlan.plankton.core.ServiceStatus;

@EqualsAndHashCode
@ToString
public class WaitFailureOf implements ServiceDependency {

    @Getter
    Service parentService;

    @Getter
    Service requiredService;

    public WaitFailureOf(Service parentService, Service requiredService) {
        this.parentService = parentService;
        this.requiredService = requiredService;
    }

    @Override
    public boolean isSatisfied() {
        return requiredService.getStatus().isFailure();
    }

    @Override
    public boolean isBlocked() {
        ServiceStatus status = requiredService.getStatus();
        return !(status.isWaiting() || status.isRunning() || status.isFailure());
    }
}
