package com.loadtest.execution.runner;

import com.loadtest.execution.domain.ExecutionMode;

import java.util.Map;

public class SshExecutionTarget implements ExecutionTarget {

    private final String host;
    private final int port;

    public SshExecutionTarget(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public ExecutionMode getMode() {
        return ExecutionMode.SSH_REMOTE;
    }

    @Override
    public Map<String, String> getEnv() {
        throw new UnsupportedOperationException(
                "SSH remote execution not implemented. Configure local target.");
    }
}
