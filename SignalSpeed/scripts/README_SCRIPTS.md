# Scripts de Ejecución para SignalSpeed

Este directorio contiene scripts para ejecutar el controlador de señales de velocidad desde binarios compilados.

## Estructura de Scripts

```
SignalSpeed/
├── compilar.sh                         # Script para compilar sin ejecutar
├── compilar_y_ejecutar.sh              # Script original (compila y ejecuta)
└── scripts/
    ├── ejecutar_signalspeed_aws.sh     # Ejecuta SignalSpeed con AWS IoT
    └── README_SCRIPTS.md               # Este archivo
```

## Requisitos Previos

1. **Compilar el proyecto primero:**
   ```bash
   cd SignalSpeed
   ./compilar.sh
   ```

2. **Conectividad MQTT:**
   - MQTT Local: `tcp://tambori.dsic.upv.es:1883`
   - Usado para recibir eventos "step" y publicar estado de la señal

3. **Certificados AWS IoT (opcional):**
   - Los certificados deben estar en `certs/`
   - Formato: `dispositivo2-certificate.pem.crt` y `dispositivo2-private.pem.key`
   - O usar certificados específicos: `{thingName}-certificate.pem.crt` y `{thingName}-private.pem.key`
   - Referencia: ver `AWS_SHADOW_CONFIG.md`

## Flujo de Trabajo

### 1. Compilar el Proyecto

```bash
cd SignalSpeed
./compilar.sh
```

Esto genera los binarios en `bin/` sin ejecutar nada.

### 2. Ejecutar SignalSpeed con AWS IoT

```bash
./scripts/ejecutar_signalspeed_aws.sh
```

O manualmente con parámetros personalizados:

```bash
java -cp "bin:lib/*" dispositivo.iniciador.SignalSpeedIniciador_AWSShadow \
    tcp://tambori.dsic.upv.es:1883 \
    R1s1 \
    SL_R1s1_001 \
    50 \
    0 \
    580 \
    SignalSpeed_SL_R1s1_001
```

## Script Disponible

### ejecutar_signalspeed_aws.sh

Ejecuta el controlador de señales de velocidad con soporte AWS IoT Device Shadow.

**Uso:**
```bash
./ejecutar_signalspeed_aws.sh
```

**Parámetros internos (edita el script para cambiarlos):**
- `MQTT_BROKER`: Dirección del broker MQTT local (default: `tcp://tambori.dsic.upv.es:1883`)
- `ROAD_SEGMENT`: Segmento de carretera (default: `R1s1`)
- `SIGNAL_ID`: ID de la señal (default: `SL_R1s1_001`)
- `VELOCIDAD_MAX`: Velocidad máxima en km/h (default: `50`)
- `POS_START`: Posición inicial en metros (default: `0`)
- `POS_END`: Posición final en metros (default: `580`)
- `AWS_THING_NAME`: Nombre del Thing en AWS IoT (default: `SignalSpeed_SL_R1s1_001`)

**Ejemplos:**

```bash
# Ejecutar con valores por defecto
./ejecutar_signalspeed_aws.sh

# Editar el script para cambiar parámetros:
# nano scripts/ejecutar_signalspeed_aws.sh
# Luego:
# ./ejecutar_signalspeed_aws.sh
```

## Funcionalidades de SignalSpeed

### 1. Publicación de Estado
- **Local (MQTT):** Publica en `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/signals`
  - Contiene: ID, segmento, posición, velocidad máxima, timestamp
- **AWS IoT:** Publica en `$aws/things/{thingName}/shadow/update`
  - Contiene: Estado reported del Device Shadow (activo, velocidad, posición, etc.)

### 2. Suscripción a Eventos
- **Local:** Se suscribe a `es/upv/pros/tatami/smartcities/traffic/PTPaterna/step`
  - Publica estado cada vez que recibe un evento "step" del simulador
- **AWS IoT:** Se suscribe a `$aws/things/{thingName}/shadow/update/delta`
  - Recibe comandos remotos para activar/desactivar la señal

### 3. Comandos Remotos (AWS IoT Device Shadow)
Puedes controlar la señal desde AWS IoT Console actualizando el estado "desired":

```json
{
  "state": {
    "desired": {
      "activate": true
    }
  }
}
```

Comandos soportados:
- `activate` (boolean): `true` para activar, `false` para desactivar la señal

### 4. Ciclo de Vida
1. Conecta al broker MQTT local
2. Se suscribe al topic de "step"
3. Habilita AWS IoT Device Shadow (si se configura)
4. Espera eventos de step y comandos remotos
5. Publica estado automáticamente en cada cambio

## Estructura de Mensajes

### Mensaje de Estado (MQTT Local)
```json
{
  "msg": {
    "signal-type": "SPEED_LIMIT",
    "rt": "traffic-signal",
    "id": "SL_R1s1_001",
    "road-segment": "R1s1",
    "starting-position": 0,
    "ending-position": 580,
    "value": 50
  },
  "id": "MSG_1638360000000",
  "type": "TRAFFIC_SIGNAL",
  "timestamp": 1638360000000
}
```

### Device Shadow (AWS IoT)
```json
{
  "state": {
    "reported": {
      "road_segment": "R1s1",
      "signal_id": "SL_R1s1_001",
      "speed_limit": 50,
      "position_start": 0,
      "position_end": 580,
      "active": true,
      "timestamp": 1638360000000
    }
  }
}
```

## Troubleshooting

### Error: "No se encontraron archivos Java"
- Verifica que los archivos `.java` estén en `src/dispositivo/`

### Error: "Error al conectar con MQTT LOCAL"
- Verifica que el broker MQTT local esté accesible en `tcp://tambori.dsic.upv.es:1883`

### Error: "Error connecting to AWS IoT"
- Verifica que los certificados estén en `certs/` con los nombres correctos
- Comprueba que el endpoint AWS IoT en el código es correcto
- Asegúrate de que el Thing existe en AWS IoT Console

### La señal no recibe comandos remotos
- Verifica que el Thing name coincida con el especificado en el script
- Comprueba que el policy del Thing permite suscribirse a `$aws/things/*/shadow/update/delta`

## Referencias

- Documentación de AWS IoT Device Shadow: `AWS_SHADOW_CONFIG.md`
- Política de seguridad: `policies/senial_SL_R1s1_001_policy-v1.json`
- Configuración general: `BUILD.md`, `README.md`
