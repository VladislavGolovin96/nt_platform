package com.loadtest.execution.runner;

import com.loadtest.execution.domain.ExecutionMode;

import java.util.Map;

public interface ExecutionTarget {
    String getHost();
    int getPort();
    ExecutionMode getMode();
    Map<String, String> getEnv();
}
