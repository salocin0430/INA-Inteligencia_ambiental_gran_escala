#!/bin/bash

# Script para ejecutar una ambulancia desde binarios compilados
# Uso: ./ejecutar_vehiculo_ambulancia.sh [id] [thingName] [velocidad] [ruta]

ID=${1:-"Ambulance001"}
THING_NAME=${2:-"SmartCar097"}
VELOCIDAD=${3:-90}
RUTA=${4:-"R1s1:0:50,R1s2:0:100"}

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
echo "Ejecutando AMBULANCIA"
echo "=========================================="
echo "ID: $ID"
echo "Thing Name: $THING_NAME"
echo "Velocidad: $VELOCIDAD km/h (ignora límites)"
echo "Ruta: $RUTA"
echo "=========================================="
echo ""

# Cambiar al directorio del proyecto para que las rutas relativas funcionen
cd "$PROJECT_DIR"

# Ejecutar
java -cp "$CLASSPATH" smartcar.starter.SmartCarStarter_Test ambulance "$ID" "$THING_NAME" "$VELOCIDAD" "$RUTA"

