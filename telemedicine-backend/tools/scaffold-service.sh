#!/usr/bin/env bash
set -euo pipefail

LANG="${1:-}"
NAME="${2:-}"

if [[ -z "$LANG" || -z "$NAME" ]]; then
  echo "Usage: $0 <java|python> <service-name>"
  exit 1
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SLUG="${NAME//_/-}"

if [[ "$LANG" == "java" ]]; then
  TARGET="$ROOT/services/$SLUG"
  cp -r "$ROOT/services/_template-java" "$TARGET"
  find "$TARGET" -type f -exec sed -i "s/template-java/$SLUG/g" {} +
  find "$TARGET" -type f -exec sed -i "s/templatejava/${SLUG//-/_}/g" {} +
  echo "Created Java service at services/$SLUG"
  echo "Add to settings.gradle: include '$SLUG' with projectDir file('services/$SLUG')"
  echo "Add gateway route for lb://$SLUG"
elif [[ "$LANG" == "python" ]]; then
  TARGET="$ROOT/services/$SLUG"
  cp -r "$ROOT/services/_template-python" "$TARGET"
  find "$TARGET" -type f -exec sed -i "s/template-python/$SLUG/g" {} +
  echo "Created Python service at services/$SLUG"
  echo "Add gateway route for lb://$SLUG"
else
  echo "Unknown language: $LANG"
  exit 1
fi
