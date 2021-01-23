package me.adarlan.plankton.compose;

public interface ComposeAdapter {

    void createContainers(ComposeService service);

    int runContainer(ComposeService service, int containerIndex);

    void stopContainer(ComposeService service, int containerIndex);
}
