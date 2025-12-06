#!/bin/bash

# Script para ejecutar un vehículo normal desde binarios compilados
# Uso: ./ejecutar_vehiculo_normal.sh [id] [thingName] [velocidad] [ruta]

ID=${1:-"SmartCar001"}
THING_NAME=${2:-"SmartCar097"}
VELOCIDAD=${3:-60}
RUTA=${4:-"R5s1:0:580,R1s4a:490:600"}

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
echo "Ejecutando Vehículo NORMAL"
echo "=========================================="
echo "ID: $ID"
echo "Thing Name: $THING_NAME"
echo "Velocidad: $VELOCIDAD km/h"
echo "Ruta: $RUTA"
echo "=========================================="
echo ""

# Cambiar al directorio del proyecto para que las rutas relativas funcionen
cd "$PROJECT_DIR"

# Ejecutar
java -cp "$CLASSPATH" smartcar.starter.SmartCarStarter_Test normal "$ID" "$THING_NAME" "$VELOCIDAD" "$RUTA"

