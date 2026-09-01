#!/bin/bash

set -e

CONNECTOR_NAME="$1"
DEBEZIUM_URL="http://localhost:8083/connectors"
CONNECTOR_DIR="./debezium"

if [ -z "$CONNECTOR_NAME" ]; then
    echo "Usage: $0 <connector-json-file>"
    echo "Example: $0 wallet-outbox-connector.json"
    exit 1
fi

CONNECTOR_FILE="$CONNECTOR_DIR/$CONNECTOR_NAME"

if [ ! -f "$CONNECTOR_FILE" ]; then
    echo "Error: connector file not found: $CONNECTOR_FILE"
    exit 1
fi

echo "Registering connector from: $CONNECTOR_FILE"

curl -i -X POST "$DEBEZIUM_URL" \
    -H "Content-Type: application/json" \
    --data @"$CONNECTOR_FILE"

echo
echo "Done."