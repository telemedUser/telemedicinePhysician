#!/bin/sh
set -eu

BOOTSTRAP="${KAFKA_BOOTSTRAP:-kafka:9092}"
TOPICS_FILE="${TOPICS_FILE:-/topics/topics.txt}"
PARTITIONS="${KAFKA_TOPIC_PARTITIONS:-3}"
REPLICATION="${KAFKA_TOPIC_REPLICATION:-1}"

echo "Waiting for Kafka at ${BOOTSTRAP}..."
until /opt/bitnami/kafka/bin/kafka-broker-api-versions.sh --bootstrap-server "${BOOTSTRAP}" >/dev/null 2>&1; do
  sleep 2
done

echo "Creating topics from ${TOPICS_FILE}..."
while IFS= read -r topic || [ -n "${topic}" ]; do
  topic=$(echo "${topic}" | tr -d '\r' | xargs)
  [ -z "${topic}" ] && continue
  [ "${topic#\#}" != "${topic}" ] && continue
  echo "  -> ${topic}"
  /opt/bitnami/kafka/bin/kafka-topics.sh \
    --bootstrap-server "${BOOTSTRAP}" \
    --create \
    --if-not-exists \
    --topic "${topic}" \
    --partitions "${PARTITIONS}" \
    --replication-factor "${REPLICATION}"
done < "${TOPICS_FILE}"

echo "Kafka topic bootstrap complete."
