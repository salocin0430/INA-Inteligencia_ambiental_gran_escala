# Scripts de Ejecución para Road Manager

Este directorio contiene scripts para ejecutar el Road Manager desde binarios compilados.

## Estructura de Scripts

```
roadmanager/
├── compilar.sh                    # Script para compilar sin ejecutar
├── compilar_y_ejecutar.sh         # Script original (compila y ejecuta)
└── scripts/
    ├── ejecutar_roadmanager.sh    # Ejecuta Road Manager
    └── README_SCRIPTS.md          # Este archivo
```

## Requisitos Previos

1. **Compilar el proyecto primero:**
   ```bash
   cd seminario3-smarttraffic/Paso1/roadmanager
   ./compilar.sh
   ```

2. **Certificados AWS IoT:**
   - Los certificados deben estar en `certs/`
   - Formato: `{thingName}-certificate.pem.crt` y `{thingName}-private.pem.key`
   - Ejemplo: `RoadManager097-certificate.pem.crt` y `RoadManager097-private.pem.key`

## Flujo de Trabajo

### 1. Compilar el Proyecto

```bash
cd seminario3-smarttraffic/Paso1/roadmanager
./compilar.sh
```

Esto genera los binarios en `bin/` sin ejecutar nada.

### 2. Ejecutar Road Manager

```bash
./scripts/ejecutar_roadmanager.sh
```

O con parámetros personalizados:

```bash
./scripts/ejecutar_roadmanager.sh RoadManager002 RoadManager097
```

## Script Disponible

### ejecutar_roadmanager.sh

Ejecuta el Road Manager que:
- Monitoriza alertas de todos los segmentos de carretera
- Retransmite alertas al canal de información
- Auto-regula velocidad según densidad de tráfico
- Gestiona señales de límite de velocidad dinámicas

**Uso:**
```bash
./ejecutar_roadmanager.sh [managerId] [thingName]
```

**Parámetros (todos opcionales):**
- `managerId`: ID del Road Manager (default: RoadManager001)
- `thingName`: Nombre del Thing en AWS IoT (default: RoadManager097)

**Ejemplos:**
```bash
# Con valores por defecto
./ejecutar_roadmanager.sh

# Especificando ambos parámetros
./ejecutar_roadmanager.sh RoadManager002 RoadManager097

# Solo cambiando el ID del manager
./ejecutar_roadmanager.sh RoadManager003
```

## Funcionalidades del Road Manager

### 1. Gestión de Alertas
- **Suscribe a:** `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/+/alerts` (simulador)
- **Suscribe a:** `smartcities/traffic/PTPaterna/road/+/alerts` (AWS IoT)
- **Publica en:** `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/info` (simulador)
- **Publica en:** `smartcities/traffic/PTPaterna/road/{segment}/info` (AWS IoT)
- **Acción:** Retransmite alertas recibidas al canal de información del segmento correspondiente

### 2. Auto-regulación de Velocidad
- **Suscribe a:** `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/+/info` (simulador)
- **Suscribe a:** `smartcities/traffic/PTPaterna/road/+/info` (AWS IoT)
- **Procesa:** Mensajes `ROAD_STATUS` con información de densidad de tráfico
- **Acción:** Crea o elimina señales `speed-limit` según el estado de densidad:
  - `No_Manouvers` o `Collapsed`: Crea señal de 20 km/h
  - `Limited_Manouvers`: Crea señal de (max-speed - 20) km/h
  - `Free_Flow` o `Mostly_Free_Flow`: Elimina señales de velocidad

### 3. Gestión de Señalización
- **Publica en:** `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/signals` (simulador)
- **Publica en:** `smartcities/traffic/PTPaterna/road/{segment}/signals` (AWS IoT)
- **Acción:** Publica señales `SPEED_LIMIT` para regular la velocidad según densidad

## Casos de Prueba

### Caso 1: Road Manager Básico
```bash
./scripts/ejecutar_roadmanager.sh
```
- Manager ID: RoadManager001
- Thing Name: RoadManager097
- Funciona con simulador y AWS IoT

### Caso 2: Road Manager con ID Personalizado
```bash
./scripts/ejecutar_roadmanager.sh RoadManager002
```
- Manager ID: RoadManager002
- Thing Name: RoadManager097 (default)

### Caso 3: Road Manager con Thing Personalizado
```bash
./scripts/ejecutar_roadmanager.sh RoadManager001 RoadManager098
```
- Manager ID: RoadManager001 (default)
- Thing Name: RoadManager098

## Notas Importantes

1. **Compilación:** Asegúrate de compilar antes de ejecutar:
   ```bash
   ./compilar.sh
   ```

2. **Certificados:** El Road Manager necesita certificados AWS IoT. Si no encuentra el certificado específico del Thing, intentará usar el certificado por defecto (`dispositivo2`).

3. **Simulador:** El simulador MQTT debe estar corriendo para que el Road Manager funcione correctamente.

4. **AWS IoT:** El Road Manager se conecta automáticamente a AWS IoT si los certificados están disponibles.

5. **Múltiples instancias:** Puedes ejecutar múltiples instancias del Road Manager con diferentes IDs, pero cada una necesita su propio Thing en AWS IoT o compartir certificados.

6. **Wildcards:** El Road Manager usa wildcards (`+`) para suscribirse a todos los segmentos simultáneamente.

## Clase Starter Parametrizable

Los scripts utilizan internamente `RoadManagerStarter_Test.java` que acepta parámetros por línea de comandos:

```bash
java -cp "$CLASSPATH" roadmanager.starter.RoadManagerStarter_Test [managerId] [thingName]
```

El script de shell es un wrapper que facilita el uso de esta clase.

