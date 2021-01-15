package me.adarlan.plankton.compose;

import java.time.Instant;

public interface ContainerState {

    boolean running();

    boolean exited();

    Instant initialInstant();

    Instant finalInstant();

    Integer exitCode();
}
