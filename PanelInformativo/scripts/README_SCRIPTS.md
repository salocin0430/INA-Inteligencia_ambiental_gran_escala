# Scripts de Ejecución para PanelInformativo

Este directorio contiene scripts para ejecutar el controlador del panel informativo desde binarios compilados.

## Estructura de Scripts

```
PanelInformativo/
├── compilar.sh                      # Script para compilar sin ejecutar
├── compilar_y_ejecutar.sh           # Script original (compila y ejecuta)
└── scripts/
    ├── ejecutar_panel_aws.sh        # Ejecuta PanelInformativo con AWS IoT
    └── README_SCRIPTS.md            # Este archivo
```

## Requisitos Previos

1. **Compilar el proyecto primero:**
   ```bash
   cd PanelInformativo
   ./compilar.sh
   ```

2. **Conectividad MQTT:**
   - MQTT Local: `tcp://tambori.dsic.upv.es:1883`
   - Usado para recibir información de tráfico y publicar datos del panel

3. **Certificados AWS IoT (opcional):**
   - Los certificados deben estar en `certs/`
   - Formato: `dispositivo2-certificate.pem.crt` y `dispositivo2-private.pem.key`
   - O usar certificados específicos: `{thingName}-certificate.pem.crt` y `{thingName}-private.pem.key`

## Flujo de Trabajo

### 1. Compilar el Proyecto

```bash
cd PanelInformativo
./compilar.sh
```

Esto genera los binarios en `bin/` sin ejecutar nada.

### 2. Ejecutar PanelInformativo con AWS IoT

```bash
./scripts/ejecutar_panel_aws.sh
```

O manualmente con parámetros personalizados:

```bash
java -cp "bin:lib/*" dispositivo.iniciador.PanelInformativoIniciador_AWSShadow \
    tcp://tambori.dsic.upv.es:1883 \
    ttmi050 \
    R1s6a \
    50 \
    panel-R1s6a-001
```

## Script Disponible

### ejecutar_panel_aws.sh

Ejecuta el controlador del panel informativo con soporte AWS IoT Device Shadow.

**Uso:**
```bash
./ejecutar_panel_aws.sh
```

**Parámetros internos (edita el script para cambiarlos):**
- `MQTT_BROKER`: Dirección del broker MQTT local (default: `tcp://tambori.dsic.upv.es:1883`)
- `TTMI_ID`: ID del panel (TTMI - Traffic and Travel information Managed Instance) (default: `ttmi050`)
- `ROAD_SEGMENT`: Segmento de carretera (default: `R1s6a`)
- `UBICACION`: Ubicación/posición en la carretera en metros (default: `50`)
- `AWS_THING_NAME`: Nombre del Thing en AWS IoT (default: `panel-R1s6a-001`)

**Ejemplos:**

```bash
# Ejecutar con valores por defecto
./ejecutar_panel_aws.sh

# Editar el script para cambiar parámetros:
# nano scripts/ejecutar_panel_aws.sh
# Luego:
# ./ejecutar_panel_aws.sh
```

## Funcionalidades de PanelInformativo

### 1. Publicación de Estado
- **Local (MQTT):** Publica en `es/upv/pros/tatami/smartcities/traffic/PTPaterna/ttmi/{ttmiID}`
  - Contiene: Estado del panel, funciones activas, información de tráfico
- **AWS IoT:** Publica en `$aws/things/{thingName}/shadow/update`
  - Contiene: Estado reported del Device Shadow (activo, ubicación, funciones, etc.)

### 2. Suscripción a Eventos
- **Local:** Se suscribe a múltiples topics MQTT:
  - `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/+/info` (información de tráfico)
  - `es/upv/pros/tatami/smartcities/traffic/PTPaterna/ttmi/+/state` (estado de paneles)
  - `es/upv/pros/tatami/smartcities/traffic/PTPaterna/step` (eventos de simulación)
- **AWS IoT:** Se suscribe a `$aws/things/{thingName}/shadow/update/delta`
  - Recibe comandos remotos para controlar el panel

### 3. Comandos Remotos (AWS IoT Device Shadow)
Puedes controlar el panel desde AWS IoT Console actualizando el estado "desired":

```json
{
  "state": {
    "desired": {
      "activo": true,
      "funcion": "mostrar_velocidad_limite"
    }
  }
}
```

Comandos soportados:
- `activo` (boolean): `true` para activar, `false` para desactivar el panel
- `funcion` (string): Función a mostrar (ej: "mostrar_velocidad_limite", "mostrar_alerta")

### 4. Ciclo de Vida
1. Conecta al broker MQTT local
2. Se suscribe a topics de información de tráfico
3. Habilita AWS IoT Device Shadow (si se configura)
4. Espera mensajes de tráfico y comandos remotos
5. Actualiza la visualización del panel automáticamente
6. Publica estado en cada cambio

### 5. Funciones del Panel
El panel puede mostrar:
- Límites de velocidad dinámicos
- Alertas de tráfico
- Estado de congestión
- Información de incidentes
- Datos de viaje en tiempo real

## Estructura de Mensajes

### Mensaje de Estado (MQTT Local)
```json
{
  "ttmiID": "ttmi050",
  "roadSegment": "R1s6a",
  "ubicacion": 50,
  "activo": true,
  "funciones_activas": [
    "mostrar_velocidad_limite",
    "mostrar_alerta_trafico"
  ],
  "timestamp": 1638360000000
}
```

### Device Shadow (AWS IoT)
```json
{
  "state": {
    "reported": {
      "ttmi_id": "ttmi050",
      "road_segment": "R1s6a",
      "ubicacion": 50,
      "activo": true,
      "funciones_activas": [
        "mostrar_velocidad_limite"
      ],
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

### El panel no recibe comandos remotos
- Verifica que el Thing name coincida con el especificado en el script
- Comprueba que el policy del Thing permite suscribirse a `$aws/things/*/shadow/update/delta`

### El panel no muestra información de tráfico
- Verifica que otros componentes (SmartCar, RoadManager) están publicando en los topics correctos
- Comprueba la conectividad MQTT local

## Referencias

- Documentación de AWS IoT Device Shadow: Consular `AWS_SHADOW_CONFIG.md` en otros módulos
- Política de seguridad: `policies/panel_informativo_R1s1_policy-v2.json`
- Configuración general: `BUILD.md`, `README.md`
