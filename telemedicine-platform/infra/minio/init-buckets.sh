#!/bin/sh
set -eu

MINIO_ENDPOINT="${MINIO_ENDPOINT:-http://minio:9000}"
MINIO_ROOT_USER="${MINIO_ROOT_USER:?MINIO_ROOT_USER required}"
MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:?MINIO_ROOT_PASSWORD required}"
ALIAS="${MINIO_ALIAS:-telemedicine}"

echo "Waiting for MinIO at ${MINIO_ENDPOINT}..."
until mc alias set "${ALIAS}" "${MINIO_ENDPOINT}" "${MINIO_ROOT_USER}" "${MINIO_ROOT_PASSWORD}" >/dev/null 2>&1; do
  sleep 2
done

for bucket in clinical-documents prescriptions audit-exports; do
  echo "Ensuring bucket: ${bucket}"
  mc mb --ignore-existing "${ALIAS}/${bucket}"
  mc version enable "${ALIAS}/${bucket}" || true
done

echo "MinIO bucket bootstrap complete."
