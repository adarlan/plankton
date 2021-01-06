package me.adarlan.plankton;

import lombok.Data;

@Data
public class PlanktonConfig {

    String name;

    String file;

    String workspace;

    // String environment;

    String metadata;

    String dockerHost;
}