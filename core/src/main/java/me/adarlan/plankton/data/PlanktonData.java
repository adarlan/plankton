package me.adarlan.plankton.data;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import me.adarlan.plankton.Pipeline;

@RequiredArgsConstructor
public class PlanktonData {

    private final Pipeline pipeline;

    public List<Job> getJobs() {
        return new ArrayList<>();
    }
}
