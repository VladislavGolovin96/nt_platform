// MongoDB initialization script
db = db.getSiblingDB('loadtest');

db.createCollection('execution_logs');
db.execution_logs.createIndex({ executionId: 1, lineNumber: 1 });
db.execution_logs.createIndex({ executionId: 1, timestamp: 1 });

print('MongoDB initialized: loadtest database and execution_logs collection ready.');
