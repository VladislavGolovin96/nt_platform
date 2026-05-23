package com.loadtest.execution.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "execution_logs")
@CompoundIndexes({
        @CompoundIndex(name = "execution_line_idx", def = "{'executionId': 1, 'lineNumber': 1}")
})
public class ExecutionLog {

    @Id
    private String id;
    private String executionId;
    private int lineNumber;
    private Instant timestamp;
    private String level;
    private String message;

    public ExecutionLog() {}

    public ExecutionLog(String executionId, int lineNumber, Instant timestamp, String level, String message) {
        this.executionId = executionId;
        this.lineNumber = lineNumber;
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getExecutionId() { return executionId; }
    public void setExecutionId(String executionId) { this.executionId = executionId; }

    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
