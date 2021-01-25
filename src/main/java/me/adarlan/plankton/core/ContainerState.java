package me.adarlan.plankton.core;

import java.time.Instant;

public interface ContainerState {

    boolean running();

    boolean exited();

    Instant initialInstant();

    Instant finalInstant();

    Integer exitCode();
}
