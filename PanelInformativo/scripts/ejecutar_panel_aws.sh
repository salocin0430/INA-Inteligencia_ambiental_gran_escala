
#!/bin/bash

# Script para ejecutar un ejemplo del iniciador AWS de PanelInformativo
# Ajusta los argumentos según tu entorno antes de ejecutar.

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

echo "Classpath: $CLASSPATH"

# Ejemplo de parámetros (modifica esto):
MQTT_BROKER="tcp://tambori.dsic.upv.es:1883"
TTMI_ID="ttmi050"
ROAD_SEGMENT="R1s6a"
UBICACION=50
AWS_THING_NAME="panel-R1s6a-001"

echo "Iniciando PanelInformativoIniciador_AWSShadow con Thing: $AWS_THING_NAME"
java -cp "$CLASSPATH" dispositivo.iniciador.PanelInformativoIniciador_AWSShadow "$MQTT_BROKER" "$TTMI_ID" "$ROAD_SEGMENT" "$UBICACION" "$AWS_THING_NAME"
