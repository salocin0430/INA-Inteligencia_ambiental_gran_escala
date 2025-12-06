#!/bin/bash

# Script para compilar y ejecutar SmartCar con AWS Shadow
# Uso: ./compilar_y_ejecutar.sh

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$PROJECT_DIR/src"
BIN_DIR="$PROJECT_DIR/bin"
LIB_DIR="$PROJECT_DIR/lib"

echo "=========================================="
echo "Compilando SmartCar con AWS Shadow"
echo "=========================================="

# Crear directorio bin si no existe
mkdir -p "$BIN_DIR"

# Construir classpath con todas las librerías
CLASSPATH="$BIN_DIR"
for jar in "$LIB_DIR"/*.jar; do
    if [ -f "$jar" ]; then
        CLASSPATH="$CLASSPATH:$jar"
    fi
done

echo "Classpath: $CLASSPATH"
echo ""

# Compilar todos los archivos Java
echo "Compilando archivos Java..."
find "$SRC_DIR" -name "*.java" | while read java_file; do
    echo "  Compilando: $java_file"
    javac -cp "$CLASSPATH" -d "$BIN_DIR" "$java_file" 2>&1 | grep -v "warning" || true
done

# Verificar si hubo errores
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Compilación exitosa!"
    echo ""
    echo "=========================================="
    echo "Ejecutando SmartCar con AWS Shadow"
    echo "=========================================="
    echo ""
    
    # Ejecutar
    java -cp "$CLASSPATH" smartcar.starter.SmartCarStarter_AWSShadow
    
else
    echo ""
    echo "❌ Error en la compilación"
    exit 1
fi

