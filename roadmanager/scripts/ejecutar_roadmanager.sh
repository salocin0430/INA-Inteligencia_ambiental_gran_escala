#!/bin/bash

# Script para ejecutar Road Manager desde binarios compilados
# Uso: ./ejecutar_roadmanager.sh [managerId] [thingName]

MANAGER_ID=${1:-"RoadManager001"}
THING_NAME=${2:-"RoadManager097"}

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BIN_DIR="$PROJECT_DIR/bin"
LIB_DIR="$PROJECT_DIR/lib"

# Construir classpath
CLASSPATH="$BIN_DIR"
for jar in "$LIB_DIR"/*.jar; do
    if [ -f "$jar" ]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

echo "=========================================="
echo "Ejecutando Road Manager"
echo "=========================================="
echo "Manager ID: $MANAGER_ID"
echo "Thing Name: $THING_NAME"
echo "=========================================="
echo ""

# Cambiar al directorio del proyecto para que las rutas relativas funcionen
cd "$PROJECT_DIR"

# Ejecutar
java -cp "$CLASSPATH" roadmanager.starter.RoadManagerStarter_Test "$MANAGER_ID" "$THING_NAME"

