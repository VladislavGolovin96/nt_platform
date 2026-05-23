package com.loadtest.common.kafka;

public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String PROJECT_BUILT    = "project.built";
    public static final String TEST_STARTED     = "test.started";
    public static final String TEST_FINISHED    = "test.finished";
    public static final String TEST_FAILED      = "test.failed";
    public static final String REPORT_GENERATED = "report.generated";

    /** Dynamic per-execution topic. Use KafkaTopics.testLog(executionId). */
    public static String testLog(String executionId) {
        return "test.log." + executionId;
    }
}
