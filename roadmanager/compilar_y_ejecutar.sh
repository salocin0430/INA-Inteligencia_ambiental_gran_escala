#!/bin/bash

# Script para compilar y ejecutar Road Manager
# Uso: ./compilar_y_ejecutar.sh

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$PROJECT_DIR/src"
BIN_DIR="$PROJECT_DIR/bin"
LIB_DIR="$PROJECT_DIR/lib"

echo "=========================================="
echo "Compilando Road Manager"
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

# Compilar todos los archivos Java en una sola pasada
echo "Compilando archivos Java..."
JAVA_FILES=()
while IFS= read -r -d '' file; do
    JAVA_FILES+=("$file")
done < <(find "$SRC_DIR" -name "*.java" -print0)

if [ ${#JAVA_FILES[@]} -gt 0 ]; then
    echo "  Compilando ${#JAVA_FILES[@]} archivos..."
    javac -cp "$CLASSPATH" -d "$BIN_DIR" "${JAVA_FILES[@]}" 2>&1 | grep -v "warning" || true
else
    echo "  No se encontraron archivos Java"
fi

# Verificar si hubo errores
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Compilación exitosa!"
    echo ""
    echo "=========================================="
    echo "Ejecutando Road Manager"
    echo "=========================================="
    echo ""
    
    # Ejecutar
    java -cp "$CLASSPATH" roadmanager.starter.RoadManagerStarter
    
else
    echo ""
    echo "❌ Error en la compilación"
    exit 1
fi

