package me.adarlan.plankton.pipeline.dependencies;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.adarlan.plankton.pipeline.Service;
import me.adarlan.plankton.pipeline.ServiceDependency;
import me.adarlan.plankton.pipeline.ServiceStatus;

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
        return requiredService.getStatus().isFailed();
    }

    @Override
    public boolean isBlocked() {
        ServiceStatus status = requiredService.getStatus();
        return !(status.isWaiting() || status.isRunning() || status.isFailed());
    }
}
