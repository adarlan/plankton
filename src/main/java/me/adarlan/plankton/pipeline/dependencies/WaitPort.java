package me.adarlan.plankton.pipeline.dependencies;

import java.io.IOException;
import java.net.Socket;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.adarlan.plankton.pipeline.Service;
import me.adarlan.plankton.pipeline.ServiceDependency;
import me.adarlan.plankton.pipeline.ServiceStatus;

@EqualsAndHashCode
@ToString
public class WaitPort implements ServiceDependency {

    @Getter
    Service parentService;

    @Getter
    Service requiredService;

    @Getter
    Integer port;

    public WaitPort(Service parentService, Service requiredService, Integer port) {
        this.parentService = parentService;
        this.requiredService = requiredService;
        this.port = port;
    }

    @Override
    public boolean isSatisfied() {
        if (requiredService.getStatus().isRunning()) {
            try (Socket s = new Socket("localhost", port)) {
                return true;
            } catch (IOException ex) {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean isBlocked() {
        ServiceStatus status = requiredService.getStatus();
        return !(status.isWaiting() || status.isRunning());
    }
}
