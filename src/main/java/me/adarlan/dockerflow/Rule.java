package me.adarlan.dockerflow;

public interface Rule {

    Job getParentJob();

    String getName();

    Object getValue();

    RuleStatus getStatus();

    boolean updateStatus();

    @lombok.Data
    public static class Data {
        String name;
        Object value;
        String status;
    }

    default Data getData() {
        Data data = new Data();
        data.name = this.getName();
        data.value = this.getValue();
        data.status = this.getStatus().toString().toLowerCase();
        return data;
    }
}