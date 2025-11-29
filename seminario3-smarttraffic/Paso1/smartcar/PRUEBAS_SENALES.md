# 🧪 Pruebas de Señales de Tráfico

## 📍 Información del Topic

**Broker MQTT:** `tcp://tambori.dsic.upv.es:10083`

**Topic base:** `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/signals`

**Ejemplo para R5s1:** 
```
es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/R5s1/signals
```

---

## 🚦 Ejemplo 1: Señal de Límite de Velocidad (SPEED_LIMIT)

### Opción A: Formato simple (sin wrapper)
```json
{
  "type": "SPEED_LIMIT",
  "value": 40,
  "position-start": 100,
  "position-end": 300,
  "validity": 3600000
}
```

### Opción B: Formato con wrapper (como otros mensajes del proyecto)
```json
{
  "msg": {
    "type": "SPEED_LIMIT",
    "value": 40,
    "position-start": 100,
    "position-end": 300,
    "validity": 3600000
  },
  "id": "MSG_1234567890",
  "type": "SIGNAL",
  "timestamp": 1234567890
}
```

**Campos:**
- `type`: **"SPEED_LIMIT"** (obligatorio)
- `value`: Velocidad máxima en km/h (obligatorio)
- `position-start`: Posición inicial donde aplica (opcional, default: 0)
- `position-end`: Posición final donde aplica (opcional, default: Integer.MAX_VALUE)
- `validity`: Validez en milisegundos (opcional)

**Ejemplo práctico:** Límite de 40 km/h entre los puntos 100 y 300 de R5s1

---

## 🚥 Ejemplo 2: Semáforo (TRAFFIC_LIGHT)

### Opción A: Formato simple (sin wrapper)
```json
{
  "type": "TRAFFIC_LIGHT",
  "state": "RED",
  "position": 200
}
```

### Opción B: Formato con wrapper
```json
{
  "msg": {
    "type": "TRAFFIC_LIGHT",
    "state": "RED",
    "position": 200
  },
  "id": "MSG_1234567890",
  "type": "SIGNAL",
  "timestamp": 1234567890
}
```

**Campos:**
- `type`: **"TRAFFIC_LIGHT"** (obligatorio)
- `state`: Estado del semáforo (obligatorio)
  - `"RED"` o `"HLL"` = Rojo (detenerse)
  - `"GREEN"` o `"LLH"` = Verde (continuar)
  - `"YELLOW"` = Amarillo (precaución)
- `position`: Posición del semáforo en el segmento (obligatorio)

**Ejemplo práctico:** Semáforo en rojo en el punto 200 de R5s1

---

## 📝 Cómo Probar

### 1. Ejecutar SmartCar
```bash
# Ejecutar SmartCarStarter_Navigator
# El vehículo se suscribirá automáticamente a:
# es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/R5s1/signals
```

### 2. Publicar mensaje con MQTT.fx o similar

**Configuración:**
- Broker: `tambori.dsic.upv.es`
- Port: `10083`
- Topic: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/R5s1/signals`
- QoS: `0`
- Retained: `false`

**Mensaje de prueba (SPEED_LIMIT 40 km/h):**
```json
{
  "type": "SPEED_LIMIT",
  "value": 40,
  "position-start": 0,
  "position-end": 580
}
```

### 3. Verificar en los logs

Deberías ver:
```
(SignalsSubscriber: SmartCar001) Received signal message from .../road/R5s1/signals: {...}
(SignalsSubscriber: SmartCar001) Added SPEED_LIMIT signal: SpeedLimitSignal{value=40 km/h, range=[0-580]}
```

Y en el siguiente paso de simulación, la velocidad debería ajustarse a 40 km/h.

---

## 🎯 Escenarios de Prueba

### Escenario 1: Límite de velocidad restrictivo
1. El vehículo circula a 60 km/h (velocidad crucero)
2. Publicar señal: `SPEED_LIMIT` con `value: 30`
3. **Resultado esperado:** El vehículo reduce su velocidad a 30 km/h

### Escenario 2: Semáforo rojo cercano
1. El vehículo está en posición 150 de R5s1
2. Publicar señal: `TRAFFIC_LIGHT` con `state: "RED"`, `position: 180`
3. **Resultado esperado:** El vehículo se detiene (velocidad = 0) porque está a 30m del semáforo (< 50m)

### Escenario 3: Semáforo rojo lejano
1. El vehículo está en posición 100 de R5s1
2. Publicar señal: `TRAFFIC_LIGHT` con `state: "RED"`, `position: 300`
3. **Resultado esperado:** El vehículo continúa (está a 200m, > 50m)

### Escenario 4: Cambio de segmento
1. El vehículo está en R5s1 con una señal activa
2. El vehículo cambia a R1s4a
3. **Resultado esperado:** Las señales de R5s1 se limpian, se suscribe a señales de R1s4a

---

## 🔍 Debugging

Si no funciona, verifica:

1. **¿El SmartCar está suscrito al topic correcto?**
   - Busca en los logs: `"Subscribed to signals topic: ..."`

2. **¿El mensaje llega al subscriber?**
   - Busca: `"Received signal message from ..."`

3. **¿Se procesa correctamente?**
   - Busca: `"Added SPEED_LIMIT signal: ..."` o `"Added TRAFFIC_LIGHT signal: ..."`

4. **¿Se aplica la velocidad?**
   - Agrega logs en `calcularVelocidadActual()` para ver qué velocidad se calcula

---

## 📌 Notas

- El código acepta mensajes **con o sin wrapper** `msg`
- Si no especificas `position-start` y `position-end`, la señal aplica a todo el segmento
- Si no especificas `validity`, se usa -1 (sin expiración)
- Las señales se limpian automáticamente al cambiar de segmento
- Solo se aplica la señal si la posición actual está dentro del rango (`position-start` a `position-end`)

