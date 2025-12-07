#!/bin/bash

# Compila y ejecuta el iniciador AWS de SignalSpeed

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Compilando proyecto..."
"$PROJECT_DIR/compilar.sh"

if [ $? -ne 0 ]; then
    echo "Error en compilación. Abortando."
    exit 1
fi

echo "Ejecutando iniciador AWS de SignalSpeed..."
"$PROJECT_DIR/scripts/ejecutar_signalspeed_aws.sh"
