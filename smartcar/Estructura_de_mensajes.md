# Contratos de Comunicación - Smart Car y Road Manager

Este documento describe todos los contratos de comunicación utilizados por el SmartCar y el Road Manager (Gestor de Carreteras), incluyendo topics MQTT (simulador y AWS IoT), endpoints REST, y formatos de mensajes.

---

## 1. Brokers MQTT

### 1.1. Simulador SmartTraffic
- **Broker:** `tcp://tambori.dsic.upv.es:10083`
- **Protocolo:** MQTT (puerto 10083)
- **Autenticación:** Sin autenticación

### 1.2. AWS IoT Core
- **Broker:** `a1knxlrh9s811y-ats.iot.us-east-1.amazonaws.com`
- **Protocolo:** MQTT over TLS (puerto 8883)
- **Autenticación:** Certificados X.509
- **Certificados:** `./certs/{thingName}-certificate.pem.crt`, `{thingName}-private.pem.key`

---

## 2. Topics MQTT - Simulador (tambori.dsic.upv.es:10083)

### 2.1. Topic: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/step`
- **Acción:** SUBSCRIBE
- **QOS:** 0
- **Descripción:** Recibe pasos de simulación del simulador
- **Publicado por:** Simulador SmartTraffic
- **Consumido por:** `SmartCar_StepSubscriber`

**Formato del mensaje:**
```json
{
  "msg": {
    "simulator": "PTPaterna",
    "step": 190428
  },
  "id": "MSG_1764890816674",
  "type": "SIMULATOR_STEP",
  "timestamp": 1764890816674
}
```

---

### 2.2. Topic: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road-segment}/traffic`
- **Acción:** PUBLISH
- **QOS:** 0
- **Descripción:** Publica eventos de tráfico (VEHICLE_IN, VEHICLE_OUT)
- **Publicado por:** `SmartCar_TrafficPublisher`
- **Consumido por:** Simulador SmartTraffic

**Formato del mensaje (VEHICLE_IN):**
```json
{
  "msg": {
    "vehicle-role": "PrivateUsage",
    "action": "VEHICLE_IN",
    "road-segment": "R5s1",
    "position": 50,
    "vehicle-id": "SmartCar001"
  },
  "id": "MSG_1764890816817",
  "type": "TRAFFIC",
  "timestamp": 1764890816817
}
```

**Formato del mensaje (VEHICLE_OUT):**
```json
{
  "msg": {
    "vehicle-role": "PrivateUsage",
    "action": "VEHICLE_OUT",
    "road-segment": "R5s1",
    "position": 580,
    "vehicle-id": "SmartCar001"
  },
  "id": "MSG_1764890816817",
  "type": "TRAFFIC",
  "timestamp": 1764890816817
}
```

**Valores posibles de `vehicle-role`:**
- `"PrivateUsage"`
- `"Ambulance"`
- `"Police"`

---

### 2.3. Topic: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road-segment}/info`
- **Acción:** SUBSCRIBE
- **QOS:** 0
- **Descripción:** Recibe información y alertas del segmento de carretera
- **Publicado por:** Simulador SmartTraffic, SmartRoad
- **Consumido por:** `SmartCar_RoadInfoSubscriber`

**Tipos de mensajes soportados:**

#### a) ROAD_STATUS (formato con wrapper)
```json
{
  "msg": {
    "code": "R1s1",
    "rt": "road-segment",
    "road-segment": "R1s1",
    "link": "/segment/R1s1",
    "road": "R1",
    "length": 50,
    "start-kp": 0,
    "end-kp": 50,
    "max-speed": 40,
    "current-max-speed": 40,
    "capacity": 6,
    "num-vehicles": 5,
    "density": 83,
    "status": "No_Manouvers"
  },
  "id": "MSG_1638980437863",
  "type": "ROAD_STATUS",
  "timestamp": 1638980437863
}
```

#### b) ACCIDENT (formato con wrapper)
```json
{
  "msg": {
    "event": "OPEN",
    "rt": "accidente",
    "id": "ACC_SmartCar001_1764890816817",
    "vehicle": "SmartCar001",
    "road-segment": "R5s1",
    "position": 250
  },
  "id": "MSG_1764890816817",
  "type": "ACCIDENT",
  "timestamp": 1764890816817
}
```

**Valores de `event`:**
- `"OPEN"` - Se abre un nuevo accidente
- `"CLOSE"` - Se cierra un accidente existente

#### c) SPEED_LIMIT (formato directo o con wrapper)
```json
{
  "type": "SPEED_LIMIT",
  "value": 40,
  "position-start": 100,
  "position-end": 300,
  "validity": 3600000
}
```

#### d) TRAFFIC_LIGHT (formato directo o con wrapper)
```json
{
  "type": "TRAFFIC_LIGHT",
  "state": "RED",
  "position": 200
}
```

**Valores de `state`:**
- `"RED"` o `"HLL"` - Rojo (detenerse)
- `"GREEN"` o `"LLH"` - Verde (continuar)
- `"YELLOW"` - Amarillo (precaución)

#### e) CONGESTION (formato directo o con wrapper)
```json
{
  "type": "CONGESTION",
  "level": 3
}
```

#### f) INCIDENT (formato directo o con wrapper)
```json
{
  "type": "INCIDENT",
  "incidentType": "accidente",
  "road": "R5s1",
  "kp": 250
}
```

---

### 2.4. Topic: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road-segment}/signals`
- **Acción:** SUBSCRIBE
- **QOS:** 0
- **Descripción:** Recibe señales de tráfico del segmento (speed-limit, traffic-light)
- **Publicado por:** Simulador SmartTraffic
- **Consumido por:** `SmartCar_SignalsSubscriber`

**Formato del mensaje (SPEED_LIMIT):**
```json
{
  "type": "SPEED_LIMIT",
  "value": 40,
  "position-start": 100,
  "position-end": 300,
  "validity": 3600000
}
```

**Formato del mensaje (TRAFFIC_LIGHT):**
```json
{
  "type": "TRAFFIC_LIGHT",
  "state": "RED",
  "position": 200
}
```

---

### 2.5. Topic: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road-segment}/alerts`
- **Acción:** PUBLISH
- **QOS:** 0
- **Descripción:** Publica alertas e incidentes en el simulador
- **Publicado por:** `SmartCar_IncidentNotifier`
- **Consumido por:** Simulador SmartTraffic, SmartRoad

**Formato del mensaje (ACCIDENT):**
```json
{
  "msg": {
    "event": "OPEN",
    "rt": "accidente",
    "id": "ACC_SmartCar001_1764890816817",
    "vehicle": "SmartCar001",
    "road-segment": "R5s1",
    "road": "R5s1",
    "position": 250,
    "kp": 250
  },
  "id": "MSG_1764890816817",
  "type": "ACCIDENT",
  "timestamp": 1764890816817
}
```

---

### 4.1. Topic: `$aws/things/{thingName}/shadow/update`
- **Acción:** PUBLISH
- **QOS:** 0
- **Descripción:** Publica estado del vehículo en Device Shadow
- **Publicado por:** `SmartCar_AWSShadowPublisher`
- **Consumido por:** AWS IoT Device Shadow Service

**Formato del mensaje:**
```json
{
  "state": {
    "reported": {
      "location": {
        "road-segment": "R5s1",
        "position": 250
      },
      "destination": {
        "road-segment": "R1s4a",
        "position": 600
      },
      "current-speed": 60,
      "cruiser-speed": 60,
      "vehicle-role": "PrivateUsage",
      "navigator-status": "ROUTING",
      "timestamp": 1764890816817
    }
  }
}
```

**Valores posibles de `navigator-status`:**
- `"ROUTING"` - Navegando activamente
- `"REACHED_DESTINATION"` - Llegó al destino
- `"STOPPED"` - Detenido
- `"WAITING"` - Esperando ruta

---

### 4.2. Topic: `$aws/things/{thingName}/shadow/update/delta`
- **Acción:** SUBSCRIBE
- **QOS:** 0
- **Descripción:** Recibe cambios en el estado desired del Device Shadow
- **Publicado por:** AWS IoT Device Shadow Service
- **Consumido por:** `SmartCar_AWSShadowSubscriber`

**Formato del mensaje (comando para cambiar velocidad):**
```json
{
  "version": 132,
  "timestamp": 1764890816,
  "state": {
    "cruiser-speed": 80
  },
  "metadata": {
    "cruiser-speed": {
      "timestamp": 1764890597
    }
  }
}
```

---

### 4.3. Topic: `$aws/things/{thingName}/shadow/update/accepted`
- **Acción:** SUBSCRIBE (opcional, para confirmaciones)
- **QOS:** 0
- **Descripción:** Confirmación de actualización del Device Shadow
- **Publicado por:** AWS IoT Device Shadow Service

---

### 4.4. Topic: `smartcities/traffic/PTPaterna/road/{road-segment}/alerts`
- **Acción:** PUBLISH
- **QOS:** 0
- **Descripción:** Publica alertas e incidentes en AWS IoT
- **Publicado por:** `SmartCar_AWSAlertsPublisher`
- **Consumido por:** Otros servicios AWS IoT

**Formato del mensaje (ACCIDENT):**
```json
{
  "msg": {
    "event": "OPEN",
    "rt": "accidente",
    "id": "ACC_SmartCar001_1764890816817",
    "vehicle": "SmartCar001",
    "road-segment": "R5s1",
    "position": 250
  },
  "id": "MSG_1764890816817",
  "type": "ACCIDENT",
  "timestamp": 1764890816817
}
```

---

### 4.5. Topic: `smartcities/traffic/PTPaterna/road/{road-segment}/info`
- **Acción:** SUBSCRIBE
- **QOS:** 0
- **Descripción:** Recibe información y alertas del segmento en AWS IoT
- **Publicado por:** Otros servicios AWS IoT
- **Consumido por:** `SmartCar_AWSInfoSubscriber`

**Tipos de mensajes soportados (mismo formato que 2.3):**
- `ROAD_STATUS`
- `ACCIDENT`
- `ALERT`
- `INCIDENT`
- `INFO`

---

### 2.6. Topic: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road-segment}/signals` (PUBLISH por Road Manager)
- **Acción:** PUBLISH
- **QOS:** 0
- **Descripción:** Road Manager publica señales de límite de velocidad para auto-regulación
- **Publicado por:** `RoadManager_InfoPublisher`
- **Consumido por:** Simulador SmartTraffic, SmartCar

**Formato del mensaje (SPEED_LIMIT - creación):**
```json
{
  "type": "SPEED_LIMIT",
  "value": 20,
  "position-start": 0,
  "position-end": 1000,
  "validity": 9223372036854775807
}
```

**Formato del mensaje (SPEED_LIMIT - eliminación):**
```json
{
  "type": "SPEED_LIMIT",
  "value": 0,
  "position-start": 0,
  "position-end": 0,
  "validity": 0
}
```

**Nota:** Road Manager publica señales speed-limit en este topic para regular la velocidad según densidad de tráfico. El valor `validity` de `9223372036854775807` (Long.MAX_VALUE) indica validez indefinida hasta que se elimine explícitamente.

---

## 3. Topics MQTT - Road Manager (Simulador)

### 3.1. Topic: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/+/alerts` (SUBSCRIBE)
- **Acción:** SUBSCRIBE (wildcard `+`)
- **QOS:** 0
- **Descripción:** Road Manager se suscribe a alertas de TODOS los segmentos usando wildcard
- **Publicado por:** SmartCar, otros vehículos
- **Consumido por:** `RoadManager_AlertsSubscriber`

**Formato del mensaje:** Mismo que 2.5 (ACCIDENT)

**Comportamiento:** Road Manager retransmite automáticamente las alertas recibidas al canal `info` del mismo segmento.

---

### 3.2. Topic: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/+/info` (SUBSCRIBE)
- **Acción:** SUBSCRIBE (wildcard `+`)
- **QOS:** 0
- **Descripción:** Road Manager se suscribe a info de TODOS los segmentos para recibir ROAD_STATUS
- **Publicado por:** Simulador SmartTraffic, SmartRoad
- **Consumido por:** `RoadManager_RoadStatusSubscriber`

**Formato del mensaje:** Mismo que 2.3 (ROAD_STATUS)

**Comportamiento:** Road Manager procesa mensajes ROAD_STATUS para auto-regulación de velocidad según densidad.

---

### 3.3. Topic: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road-segment}/info` (PUBLISH)
- **Acción:** PUBLISH
- **QOS:** 0
- **Descripción:** Road Manager retransmite alertas al canal info
- **Publicado por:** `RoadManager_InfoPublisher`
- **Consumido por:** SmartCar, otros componentes

**Formato del mensaje:** Mismo formato que la alerta original recibida en el canal alerts (ver 2.5)

**Comportamiento:** Road Manager retransmite las alertas recibidas en el canal `alerts` al canal `info` del mismo segmento, preservando el formato original del mensaje.

---

## 5. Topics MQTT - AWS IoT Core (Road Manager)

### 5.1. Topic: `smartcities/traffic/PTPaterna/road/+/alerts` (SUBSCRIBE)
- **Acción:** SUBSCRIBE (wildcard `+`)
- **QOS:** 0
- **Descripción:** Road Manager se suscribe a alertas de TODOS los segmentos en AWS IoT
- **Publicado por:** SmartCar, otros vehículos
- **Consumido por:** `RoadManager_AWSAlertsSubscriber`

**Formato del mensaje:** Mismo que 4.4 (ACCIDENT)

**Comportamiento:** Road Manager retransmite automáticamente las alertas recibidas al canal `info` del mismo segmento en AWS IoT.

---

### 5.2. Topic: `smartcities/traffic/PTPaterna/road/{road-segment}/info` (PUBLISH)
- **Acción:** PUBLISH
- **QOS:** 0
- **Descripción:** Road Manager retransmite alertas al canal info en AWS IoT
- **Publicado por:** `RoadManager_AWSInfoPublisher`
- **Consumido por:** SmartCar, otros servicios AWS IoT

**Formato del mensaje:** Mismo formato que la alerta original recibida en el canal alerts (ver 4.4)

**Comportamiento:** Road Manager retransmite las alertas recibidas en el canal `alerts` al canal `info` del mismo segmento, preservando el formato original.

---

### 5.3. Topic: `smartcities/traffic/PTPaterna/road/+/info` (SUBSCRIBE)
- **Acción:** SUBSCRIBE (wildcard `+`)
- **QOS:** 0
- **Descripción:** Road Manager se suscribe a info de TODOS los segmentos para recibir ROAD_STATUS en AWS IoT
- **Publicado por:** Otros servicios AWS IoT
- **Consumido por:** `RoadManager_AWSRoadStatusSubscriber`

**Formato del mensaje:** Mismo que 4.5 (ROAD_STATUS)

**Comportamiento:** Road Manager procesa mensajes ROAD_STATUS para auto-regulación de velocidad según densidad.

---

### 5.4. Topic: `smartcities/traffic/PTPaterna/road/{road-segment}/signals` (PUBLISH)
- **Acción:** PUBLISH
- **QOS:** 0
- **Descripción:** Road Manager publica señales de límite de velocidad en AWS IoT
- **Publicado por:** `RoadManager_AWSInfoPublisher`
- **Consumido por:** SmartCar, otros servicios AWS IoT

**Formato del mensaje (SPEED_LIMIT - creación):**
```json
{
  "type": "SPEED_LIMIT",
  "value": 20,
  "position-start": 0,
  "position-end": 1000,
  "validity": 9223372036854775807
}
```

**Formato del mensaje (SPEED_LIMIT - eliminación):**
```json
{
  "type": "SPEED_LIMIT",
  "value": 0,
  "position-start": 0,
  "position-end": 0,
  "validity": 0
}
```

---

## 6. Endpoints REST API

### 6.1. Endpoint: `GET http://tambori.dsic.upv.es:10082/segment/{road-segment}/max-speed`
- **Descripción:** Obtiene la velocidad máxima permitida en un segmento de carretera
- **Método:** GET
- **Autenticación:** No requiere
- **Usado por:** `SmartCar.calcularVelocidadActual()`

**Parámetros:**
- `{road-segment}`: Identificador del segmento (ej: "R5s1", "R1s4a")

**Respuesta (JSON):**
```json
{
  "road-segment": "R5s1",
  "max-speed": 60
}
```

**Ejemplo:**
```
GET http://tambori.dsic.upv.es:10082/segment/R5s1/max-speed
```

**Respuesta:**
```json
{
  "road-segment": "R5s1",
  "max-speed": 60
}
```

---

## 7. Resumen de Contratos por Componente

### 7.1. SmartCar

| Componente | Acción | Topic/Endpoint | Broker |
|------------|--------|----------------|--------|
| `SmartCar_StepSubscriber` | SUBSCRIBE | `es/upv/pros/tatami/smartcities/traffic/PTPaterna/step` | Simulador |
| `SmartCar_TrafficPublisher` | PUBLISH | `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/traffic` | Simulador |
| `SmartCar_RoadInfoSubscriber` | SUBSCRIBE | `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/info` | Simulador |
| `SmartCar_SignalsSubscriber` | SUBSCRIBE | `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/signals` | Simulador |
| `SmartCar_IncidentNotifier` | PUBLISH | `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/alerts` | Simulador |
| `SmartCar_AWSShadowPublisher` | PUBLISH | `$aws/things/{thingName}/shadow/update` | AWS IoT |
| `SmartCar_AWSShadowSubscriber` | SUBSCRIBE | `$aws/things/{thingName}/shadow/update/delta` | AWS IoT |
| `SmartCar_AWSAlertsPublisher` | PUBLISH | `smartcities/traffic/PTPaterna/road/{segment}/alerts` | AWS IoT |
| `SmartCar_AWSInfoSubscriber` | SUBSCRIBE | `smartcities/traffic/PTPaterna/road/{segment}/info` | AWS IoT |
| `SmartCar` (REST) | GET | `http://tambori.dsic.upv.es:10082/segment/{segment}/max-speed` | REST API |

### 7.2. Road Manager

| Componente | Acción | Topic/Endpoint | Broker |
|------------|--------|----------------|--------|
| `RoadManager_AlertsSubscriber` | SUBSCRIBE | `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/+/alerts` | Simulador |
| `RoadManager_InfoPublisher` | PUBLISH | `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/info` | Simulador |
| `RoadManager_InfoPublisher` | PUBLISH | `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/signals` | Simulador |
| `RoadManager_RoadStatusSubscriber` | SUBSCRIBE | `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/+/info` | Simulador |
| `RoadManager_AWSAlertsSubscriber` | SUBSCRIBE | `smartcities/traffic/PTPaterna/road/+/alerts` | AWS IoT |
| `RoadManager_AWSInfoPublisher` | PUBLISH | `smartcities/traffic/PTPaterna/road/{segment}/info` | AWS IoT |
| `RoadManager_AWSInfoPublisher` | PUBLISH | `smartcities/traffic/PTPaterna/road/{segment}/signals` | AWS IoT |
| `RoadManager_AWSRoadStatusSubscriber` | SUBSCRIBE | `smartcities/traffic/PTPaterna/road/+/info` | AWS IoT |

**Nota:** Road Manager usa wildcards (`+`) para suscribirse a TODOS los segmentos simultáneamente.

---

## 8. Notas Importantes

### 8.1. Variables de segmento
- `{road-segment}`: Identificador del segmento (ej: "R5s1", "R1s4a")
- `{thingName}`: Nombre del Thing en AWS IoT (ej: "SmartCar097")

### 8.2. QOS
- Todos los topics usan **QOS 0** (at-most-once delivery)
- No se requiere garantía de entrega

### 8.3. Formato de mensajes
- Los mensajes del simulador pueden tener formato con wrapper (`msg`, `id`, `type`, `timestamp`) o directo
- Los mensajes de AWS IoT Device Shadow siguen el formato estándar de AWS
- Los mensajes de alertas en AWS IoT siguen el formato del simulador

### 8.4. Suscripciones dinámicas
- Las suscripciones a topics de segmento se actualizan automáticamente cuando el vehículo cambia de segmento
- Se desuscribe del segmento anterior y se suscribe al nuevo

### 8.5. Certificados AWS IoT

**SmartCar:**
- **Ubicación:** `./certs/`
- **Formato:** `{thingName}-certificate.pem.crt`, `{thingName}-private.pem.key`
- **CA:** `AmazonRootCA1.pem`
- Si no existe certificado específico, se usa `dispositivo2-certificate.pem.crt` por defecto

**Road Manager:**
- **Ubicación:** `./certs/`
- **Formato:** `RoadManager097-certificate.pem.crt`, `RoadManager097-private.pem.key`
- **CA:** `AmazonRootCA1.pem` o `AmazonRootCA3.pem`
- Si no existe certificado específico, se usa `dispositivo2-certificate.pem.crt` por defecto

### 8.6. Wildcards en Road Manager
- Road Manager usa wildcards (`+`) para suscribirse a múltiples segmentos simultáneamente
- `road/+/alerts` - Recibe alertas de todos los segmentos
- `road/+/info` - Recibe info de todos los segmentos
- Esto permite que un único Road Manager gestione todos los segmentos de la red

---

## 9. Ejemplos de Uso

### 9.1. Publicar evento de tráfico (VEHICLE_IN)
**Topic:** `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/R5s1/traffic`

**Mensaje:**
```json
{
  "msg": {
    "vehicle-role": "PrivateUsage",
    "action": "VEHICLE_IN",
    "road-segment": "R5s1",
    "position": 50,
    "vehicle-id": "SmartCar001"
  },
  "id": "MSG_1234567890",
  "type": "TRAFFIC",
  "timestamp": 1234567890
}
```

---

### 9.2. Publicar alerta de accidente
**Topic (Simulador):** `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/R5s1/alerts`  
**Topic (AWS IoT):** `smartcities/traffic/PTPaterna/road/R5s1/alerts`

**Mensaje:**
```json
{
  "msg": {
    "event": "OPEN",
    "rt": "accidente",
    "id": "ACC_SmartCar001_1234567890",
    "vehicle": "SmartCar001",
    "road-segment": "R5s1",
    "position": 250
  },
  "id": "MSG_1234567890",
  "type": "ACCIDENT",
  "timestamp": 1234567890
}
```

---

### 9.3. Publicar estado en Device Shadow
**Topic:** `$aws/things/SmartCar097/shadow/update`

**Mensaje:**
```json
{
  "state": {
    "reported": {
      "location": {
        "road-segment": "R5s1",
        "position": 250
      },
      "destination": {
        "road-segment": "R1s4a",
        "position": 600
      },
      "current-speed": 60,
      "cruiser-speed": 60,
      "vehicle-role": "PrivateUsage",
      "navigator-status": "ROUTING",
      "timestamp": 1234567890
    }
  }
}
```

---

### 9.4. Enviar comando para cambiar velocidad
**Desde AWS Console o MQTT Explorer:**

**Topic:** `$aws/things/SmartCar097/shadow/update`

**Mensaje:**
```json
{
  "state": {
    "desired": {
      "cruiser-speed": 80
    }
  }
}
```

El vehículo recibirá el delta en: `$aws/things/SmartCar097/shadow/update/delta`

---

### 9.5. Road Manager retransmite alerta

**Flujo:**
1. SmartCar publica alerta en: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/R5s1/alerts`
2. Road Manager recibe la alerta (suscrito a `road/+/alerts`)
3. Road Manager retransmite al canal info: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/R5s1/info`

**Mensaje original (alerts):**
```json
{
  "msg": {
    "event": "OPEN",
    "rt": "accidente",
    "id": "ACC_SmartCar001_1234567890",
    "vehicle": "SmartCar001",
    "road-segment": "R5s1",
    "position": 250
  },
  "id": "MSG_1234567890",
  "type": "ACCIDENT",
  "timestamp": 1234567890
}
```

**Mensaje retransmitido (info):** Mismo formato, preservado tal cual.

---

### 9.6. Road Manager crea señal speed-limit por densidad

**Cuando recibe ROAD_STATUS con status="No_Manouvers":**

Road Manager publica en: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/R5s1/signals`

**Mensaje:**
```json
{
  "type": "SPEED_LIMIT",
  "value": 20,
  "position-start": 0,
  "position-end": 1000,
  "validity": 9223372036854775807
}
```

**Cuando recibe ROAD_STATUS con status="Free_Flow":**

Road Manager elimina la señal publicando:
```json
{
  "type": "SPEED_LIMIT",
  "value": 0,
  "position-start": 0,
  "position-end": 0,
  "validity": 0
}
```

---

---

## 10. Políticas AWS IoT

### 10.1. Política para SmartCar

La política `smartcar_shadow_policy.json` debe permitir:

- **iot:Connect** con client IDs: `dispositivo2-*`, `SmartCar*-*`, `smartcar-*`
- **iot:Publish** y **iot:Receive** en topics de Shadow y canales alerts/info
- **iot:Subscribe** en topicfilters de Shadow y canal info

Ver archivo: `policies/smartcar_shadow_policy.json`

### 10.2. Política para Road Manager

La política `smartroad_policy.json` debe permitir:

- **iot:Connect** con client IDs: `road-manager-*`, `RoadManager-*`, `smartroad-*`, `SmartRoad-*`, `gestor-carreteras-*`
- **iot:Publish** y **iot:Receive** en topic: `smartcities/traffic/PTPaterna/road/*/info`
- **iot:Subscribe** y **iot:Receive** en topicfilter: `smartcities/traffic/PTPaterna/road/*/alerts`
- **iot:Subscribe** y **iot:Receive** en topicfilter: `smartcities/traffic/PTPaterna/road/*/info`

Ver archivo: `policies/smartroad_policy.json`

---

**Fin del documento**

