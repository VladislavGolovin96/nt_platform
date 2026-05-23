package com.loadtest.execution.runner;

import com.loadtest.execution.domain.ExecutionMode;

import java.util.Collections;
import java.util.Map;

public class LocalExecutionTarget implements ExecutionTarget {

    @Override
    public String getHost() {
        return "localhost";
    }

    @Override
    public int getPort() {
        return 0;
    }

    @Override
    public ExecutionMode getMode() {
        return ExecutionMode.LOCAL;
    }

    @Override
    public Map<String, String> getEnv() {
        return Collections.emptyMap();
    }
}
