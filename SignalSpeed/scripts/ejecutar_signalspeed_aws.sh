#!/bin/bash

# Script para ejecutar un ejemplo del iniciador AWS de SignalSpeed
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
MQTT_BROKER="tcp://tambori.dsic.upv.es:10083"
ROAD_SEGMENT="R1s1"
SIGNAL_ID="SL_R1s1_001"
VELOCIDAD_MAX=50
POS_START=0
POS_END=580
AWS_THING_NAME="SignalSpeed_SL_R1s1_001"

echo "Iniciando SignalSpeedIniciador_AWSShadow con Thing: $AWS_THING_NAME"
java -cp "$CLASSPATH" dispositivo.iniciador.SignalSpeedIniciador_AWSShadow "$MQTT_BROKER" "$ROAD_SEGMENT" "$SIGNAL_ID" "$VELOCIDAD_MAX" "$POS_START" "$POS_END" "$AWS_THING_NAME"
