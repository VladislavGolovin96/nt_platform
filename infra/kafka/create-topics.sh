#!/bin/bash
# Create all Kafka topics for the Load Testing Platform
# Runs once after Kafka is healthy (via kafka-init service)

BOOTSTRAP=kafka:9092

echo "Waiting for Kafka to be ready..."
sleep 5

create_topic() {
  local topic=$1
  local partitions=${2:-1}
  local replication=${3:-1}
  kafka-topics --bootstrap-server $BOOTSTRAP \
    --create --if-not-exists \
    --topic "$topic" \
    --partitions "$partitions" \
    --replication-factor "$replication"
  echo "Topic created (or already exists): $topic"
}

create_topic "project.built"
create_topic "test.started"
create_topic "test.finished"
create_topic "test.failed"
create_topic "report.generated"

# test.log.{executionId} topics are created dynamically by Execution Service
# but we create a template topic here for documentation purposes
# create_topic "test.log.template"

echo "All Kafka topics created successfully."
kafka-topics --bootstrap-server $BOOTSTRAP --list
