package me.adarlan.plankton.core;

import java.util.Set;

public interface Pipeline {

    String getId();

    Service getServiceByName(String name);

    Set<Service> getServices();

    void run() throws InterruptedException;
}
