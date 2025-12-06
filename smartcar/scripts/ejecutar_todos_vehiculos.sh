#!/bin/bash

# Script para ejecutar múltiples vehículos simultáneamente en terminales separadas
# Uso: ./ejecutar_todos_vehiculos.sh

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCRIPTS_DIR="$PROJECT_DIR/scripts"

echo "=========================================="
echo "Ejecutando todos los vehículos de prueba"
echo "=========================================="
echo ""
echo "Este script abrirá 3 terminales separadas:"
echo "  1. Vehículo Normal"
echo "  2. Vehículo Policía"
echo "  3. Ambulancia"
echo ""
echo "Presiona Enter para continuar o Ctrl+C para cancelar..."
read

# Verificar que los scripts existen
if [ ! -f "$SCRIPTS_DIR/ejecutar_vehiculo_normal.sh" ]; then
    echo "❌ Error: No se encuentra ejecutar_vehiculo_normal.sh"
    exit 1
fi

# Función para abrir nueva terminal (macOS)
open_terminal() {
    local script_path=$1
    local title=$2
    
    if command -v osascript > /dev/null; then
        # macOS
        osascript -e "tell application \"Terminal\" to do script \"cd '$PROJECT_DIR' && bash '$script_path'\""
    elif command -v gnome-terminal > /dev/null; then
        # Linux (GNOME)
        gnome-terminal --title="$title" -- bash -c "cd '$PROJECT_DIR' && bash '$script_path'; exec bash"
    elif command -v xterm > /dev/null; then
        # Linux (XTerm)
        xterm -title "$title" -e "cd '$PROJECT_DIR' && bash '$script_path" &
    else
        echo "⚠️  No se puede abrir terminal automáticamente. Ejecuta manualmente:"
        echo "  Terminal 1: cd $PROJECT_DIR && bash $SCRIPTS_DIR/ejecutar_vehiculo_normal.sh"
        echo "  Terminal 2: cd $PROJECT_DIR && bash $SCRIPTS_DIR/ejecutar_vehiculo_policia.sh"
        echo "  Terminal 3: cd $PROJECT_DIR && bash $SCRIPTS_DIR/ejecutar_vehiculo_ambulancia.sh"
        exit 1
    fi
}

echo "Abriendo terminales..."

# Abrir terminales
open_terminal "$SCRIPTS_DIR/ejecutar_vehiculo_normal.sh" "Vehículo Normal"
sleep 1

open_terminal "$SCRIPTS_DIR/ejecutar_vehiculo_policia.sh" "Vehículo Policía"
sleep 1

open_terminal "$SCRIPTS_DIR/ejecutar_vehiculo_ambulancia.sh" "Ambulancia"

echo ""
echo "✅ Terminales abiertas"
echo "Cada vehículo se ejecuta en su propia terminal"
echo "Presiona Ctrl+C en cada terminal para detener el vehículo correspondiente"

