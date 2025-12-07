# INA — Inteligencia Ambiental a Gran Escala

Sistema de gestión de tráfico inteligente que integra múltiples componentes distribuidos para monitorización, control y regulación en tiempo real de flujo vehicular usando MQTT local y AWS IoT.

## 🎯 Visión General

El proyecto INA combina:
- **Vehículos inteligentes** (SmartCar) que navegan y reportan información de tráfico
- **Gestor de carreteras** (RoadManager) que regula dinámicamente límites de velocidad
- **Señales de velocidad** (SignalSpeed) controladas remotamente vía AWS IoT
- **Paneles informativos** (PanelInformativo) que muestran alertas y estado de tráfico

Todo está interconectado mediante un broker MQTT local y AWS IoT Device Shadow para comandos remotos.

## 📁 Estructura del Proyecto

```
INA-Inteligencia_ambiental_gran_escala/
├── smartcar/                    # Vehículos inteligentes
│   ├── src/ina/vehicle/         # Navegación y ruteo
│   ├── src/smartcar/impl/       # Publicadores/suscriptores MQTT y AWS
│   ├── src/smartcar/starter/    # Iniciadores (básico, navigator, AWS)
│   ├── compilar.sh              # Script de compilación
│   ├── BUILD.md                 # Instrucciones de build/run
│   ├── README_SCRIPTS.md        # Documentación de scripts de ejecución
│   ├── scripts/                 # Scripts de ejecución por tipo de vehículo
│   ├── certs/                   # Certificados AWS IoT
│   ├── policies/                # Políticas de seguridad AWS
│   └── lib/                     # Librerías específicas del módulo
│
├── roadmanager/                 # Gestor centralizado de carreteras
│   ├── src/roadmanager/impl/    # Lógica de gestión de alertas y velocidad
│   ├── src/roadmanager/starter/ # Iniciadores
│   ├── compilar.sh
│   ├── BUILD.md
│   ├── README_SCRIPTS.md
│   ├── scripts/                 # Scripts de ejecución
│   ├── certs/                   # Certificados AWS IoT
│   └── lib/                     # Librerías específicas del módulo
│
├── SignalSpeed/                 # Controlador de señales de velocidad
│   ├── src/dispositivo/         # Componentes, API MQTT/REST, interfaces
│   │   ├── componentes/         # Clases principales (SignalSpeed, AWS Publisher/Subscriber)
│   │   ├── iniciador/           # Iniciadores (básico, AWS Shadow)
│   │   ├── api/                 # APIs MQTT y REST
│   │   └── interfaces/          # Interfaces y configuración
│   ├── compilar.sh
│   ├── BUILD.md
│   ├── README_SCRIPTS.md
│   ├── AWS_SHADOW_CONFIG.md     # Configuración AWS IoT Device Shadow
│   ├── scripts/                 # Scripts de ejecución con AWS
│   ├── certs/                   # Certificados AWS IoT
│   ├── policies/                # Políticas de seguridad AWS
│   └── lib/                     # Librerías específicas del módulo
│
├── PanelInformativo/            # Controlador de paneles informativos
│   ├── src/dispositivo/         # Componentes, API MQTT/REST, interfaces
│   │   ├── componentes/         # Clases principales (PanelInformativo, AWS Publisher/Subscriber)
│   │   ├── iniciador/           # Iniciadores (básico, AWS Shadow)
│   │   ├── api/                 # APIs MQTT y REST
│   │   └── interfaces/          # Interfaces y configuración
│   ├── compilar.sh
│   ├── BUILD.md
│   ├── README_SCRIPTS.md
│   ├── scripts/                 # Scripts de ejecución con AWS
│   ├── certs/                   # Certificados AWS IoT
│   ├── policies/                # Políticas de seguridad AWS
│   └── lib/                     # Librerías específicas del módulo
│
├── lib/                         # Librerías compartidas (MQTT, JSON, AWS SDK)
├── ejemplos_rutas_navigator.md  # Ejemplos de definición de rutas
├── roads_example.json           # Ejemplo de configuración de carreteras
└── README.md                    # Este archivo
```

## 🔧 Componentes Principales

### 1. SmartCar — Vehículos Inteligentes
Vehículos que navegan por rutas definidas, respetan límites de velocidad y reportan tráfico.

**Características:**
- Navegación por rutas configurables
- Respeta señales de tráfico y límites de velocidad dinámicos
- Publica información de posición, velocidad y densidad de tráfico
- Recibe información de incidentes (alertas, congestión)
- Integración con AWS IoT para control remoto
- Tipos: Normal, Policía (ignora límites), Ambulancia (ignora límites)

**Ejecutar:**
```bash
cd smartcar
./compilar.sh
./scripts/ejecutar_vehiculo_normal.sh        # Vehículo normal
./scripts/ejecutar_vehiculo_policia.sh       # Policía
./scripts/ejecutar_vehiculo_ambulancia.sh    # Ambulancia
./scripts/ejecutar_todos_vehiculos.sh        # Los 3 simultáneamente
```

### 2. RoadManager — Gestor de Carreteras
Centraliza información de tráfico y regula dinámicamente límites de velocidad según densidad.

**Características:**
- Monitoriza alertas de todos los segmentos de carretera
- Recibe información de densidad de tráfico de los vehículos
- Auto-regula límites de velocidad:
  - `Free_Flow` → sin límite (señales desactivadas)
  - `Limited_Manouvers` → velocidad reducida
  - `Collapsed` → 20 km/h (máximo control)
- Retransmite alertas e información a paneles
- Integración con AWS IoT

**Ejecutar:**
```bash
cd roadmanager
./compilar.sh
./scripts/ejecutar_roadmanager.sh
```

### 3. SignalSpeed — Señales de Velocidad Dinámica
Controla límites de velocidad en zonas específicas, sincronizable con AWS IoT.

**Características:**
- Publica límites de velocidad en segmentos de carretera
- Puede ser activada/desactivada remotamente vía AWS
- Integración total con Device Shadow de AWS IoT
- Suscrita a eventos `step` del simulador local

**Ejecutar:**
```bash
cd SignalSpeed
./compilar.sh
./scripts/ejecutar_signalspeed_aws.sh        # Con AWS IoT habilitado
```

### 4. PanelInformativo — Paneles de Información
Muestra alertas y estado del tráfico a los conductores.

**Características:**
- Publica información de tráfico y alertas
- Recibe comandos para mostrar diferentes contenidos
- Control remoto vía AWS IoT Device Shadow
- Adaptable a múltiples ubicaciones

**Ejecutar:**
```bash
cd PanelInformativo
./compilar.sh
./scripts/ejecutar_panel_aws.sh              # Con AWS IoT habilitado
```

## 📡 Flujo de Datos

### Brokers MQTT
- **Local:** `tcp://tambori.dsic.upv.es:1883` — Topics simulador, vehículos, paneles
- **AWS IoT:** AWS IoT Core MQTT — Device Shadow para comandos remotos

### Topics MQTT Principales

**Vehículos → RoadManager:**
- `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/info` — Densidad, incidentes

**RoadManager → Señales/Paneles:**
- `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/signals` — Límites de velocidad
- `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/info` — Alertas

**Simulador → Todos:**
- `es/upv/pros/tatami/smartcities/traffic/PTPaterna/step` — Evento de paso (sincronización)

### AWS IoT Device Shadow
Cada dispositivo (SmartCar, SignalSpeed, PanelInformativo) publica/recibe en:
- **Reported:** `$aws/things/{thingName}/shadow/update` — Estado actual
- **Delta:** `$aws/things/{thingName}/shadow/update/delta` — Comandos remotos

## 🚀 Guía Rápida de Inicio

### Compilación
Cada módulo tiene su script de compilación independiente:
```bash
# SmartCar
cd smartcar && ./compilar.sh

# RoadManager
cd roadmanager && ./compilar.sh

# SignalSpeed
cd SignalSpeed && ./compilar.sh

# PanelInformativo
cd PanelInformativo && ./compilar.sh
```

### Ejecución en Bash/WSL/Git Bash
```bash
# Terminal 1: RoadManager
cd roadmanager && ./scripts/ejecutar_roadmanager.sh

# Terminal 2: Vehículos (simultáneamente)
cd smartcar && ./scripts/ejecutar_todos_vehiculos.sh

# Terminal 3: Señales (opcional, con AWS)
cd SignalSpeed && ./scripts/ejecutar_signalspeed_aws.sh

# Terminal 4: Paneles (opcional, con AWS)
cd PanelInformativo && ./scripts/ejecutar_panel_aws.sh
```

### Ejecución en PowerShell (Windows)
```powershell
# Opción 1: Si tienes Git Bash
bash ./roadmanager/scripts/ejecutar_roadmanager.sh

# Opción 2: Con javac directo (ver BUILD.md de cada módulo)
javac -d smartcar\bin -cp smartcar\bin;smartcar\lib\* (lista .java)
java -cp "smartcar\bin;smartcar\lib\*" smartcar.starter.SmartCarStarter
```

### Debug en VS Code
En `.vscode/launch.json` hay configuraciones listas:
- "SmartCar - Básico"
- "SmartCar - AWS Shadow"
- "RoadManager - Básico"
- "SignalSpeed - AWS Shadow"
- "PanelInformativo - AWS Shadow"

Selecciona en el menú Debug → Run and Debug → elige configuración.

## 🔐 Configuración AWS IoT

Para usar Device Shadow (comandos remotos):

1. **Certificados:** Coloca en `{módulo}/certs/`
   - `dispositivo2-certificate.pem.crt`
   - `dispositivo2-private.pem.key`

2. **Thing creado en AWS IoT Console** con el nombre especificado en los scripts

3. **Policy con permisos** para:
   - `iot:Connect`
   - `iot:Publish`
   - `iot:Subscribe`
   - `iot:Receive` (para shadow topics)

Ver `SignalSpeed/AWS_SHADOW_CONFIG.md` y `SignalSpeed/policies/` para ejemplos.

## 🛠 Tecnologías

- **Lenguaje:** Java 8+
- **MQTT:** Eclipse Paho Client
- **AWS IoT:** AWS SDK for IoT
- **JSON:** org.json
- **REST:** Restlet Framework
- **Logging:** Logger personalizado (MySimpleLogger)
- **Build:** Bash scripts (compilar.sh)

## 📚 Documentación Adicional

- `BUILD.md` en cada módulo — Instrucciones de compilación específicas
- `README_SCRIPTS.md` en cada módulo/scripts/ — Detalles de cada script de ejecución
- `AWS_SHADOW_CONFIG.md` en SignalSpeed — Configuración AWS IoT Device Shadow
- `Estructura_de_mensajes.md` en smartcar — Formato de mensajes JSON
- `ejemplos_rutas_navigator.md` — Ejemplos de definición de rutas para vehículos
- `roads_example.json` — Configuración de ejemplo de segmentos de carretera

## 📝 Notas Importantes

- **MQTT Local es crítico:** El simulador y todos los componentes usan el broker local. Si no está disponible, habrá timeouts.
- **Sincronización:** Todos los componentes se sincronizan via topic `step` del simulador.
- **AWS es opcional:** Los componentes funcionan sin AWS. Device Shadow solo añade control remoto.
- **Certificados para AWS:** Solo necesarios si vas a usar AWS IoT (Device Shadow).

## ✅ Verificación

Para comprobar que todo funciona:
1. Broker MQTT local debe estar activo
2. Compilar sin errores: `./compilar.sh`
3. Ejecutar RoadManager primero (espera a que esté listo)
4. Luego ejecutar SmartCar(s)
5. Observar logs de publicación/suscripción MQTT

## 📧 Contacto y Referencias

Proyecto del curso **INA (Inteligencia Ambiental)** — UPV
