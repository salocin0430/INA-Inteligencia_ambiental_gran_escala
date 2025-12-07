# SignalSpeed - AWS IoT Integration

## Descripción
SignalSpeed es una señal de límite de velocidad que se integra con **AWS IoT Device Shadow** para permitir:
- Monitoreo remoto del estado de la señal desde AWS IoT Console
- Activación/desactivación remota de la señal desde AWS
- Publicación automática del estado en cada paso de simulación

## Requisitos previos

### 1. Certificados AWS IoT
Asegúrate de tener los certificados en la carpeta `certs/`:
- `dispositivo2-certificate.pem.crt` - Certificado público
- `dispositivo2-private.pem.key` - Clave privada
- `AmazonRootCA1.pem` - Certificado raíz de Amazon (si aplica)

Estos certificados deben estar registrados en AWS IoT y tener permisos suficientes.

### 2. AWS Thing
Crea un "Thing" en AWS IoT Console con el nombre que usarás en la aplicación (ej: `SignalSpeed_SL_R1s1_001`).

### 3. Policy
Asocia una policy al Thing que permita:
- Suscripción a tópicos shadow
- Publicación en tópicos shadow
- Conexión MQTT

Ver ejemplo en `policies/senial_SL_R1s1_001_policy-v1.json`

## Uso

### Opción 1: Iniciador con parámetros (básico)
```bash
java dispositivo.iniciador.SignalSpeedIniciador \
    tcp://tambori.dsic.upv.es:10083 \
    SL_R1s1_001 \
    R1s1 \
    50 \
    0 \
    580 \
    SignalSpeed_SL_R1s1_001
```

Parámetros:
1. MQTT Broker URL
2. ID de la señal
3. Segmento de carretera (ej: R1s1)
4. Velocidad máxima (km/h)
5. Posición inicio (metros)
6. Posición fin (metros)
7. **AWS Thing Name** (opcional - si se proporciona, habilita AWS IoT)

### Opción 2: Iniciador AWS Shadow (recomendado)
```bash
java dispositivo.iniciador.SignalSpeedIniciador_AWSShadow
```

Este iniciador viene preconfigurado con valores de ejemplo y habilita automáticamente la integración AWS.

## Configuración

### En SignalSpeed.java
La clase `SignalSpeed` ofrece los siguientes métodos para integración AWS:

```java
// Habilitar AWS IoT Device Shadow
signal.enableAWSShadow("SignalSpeed_SL_R1s1_001");

// Activar/desactivar la señal
signal.activarSeñal();
signal.desactivarSeñal();

// Obtener estado
boolean active = signal.isSeñalActiva();

// Cerrar conexiones
signal.cerrar();
```

### En AWS IoT Console

#### Enviar comandos a la señal
1. Ve a **AWS IoT Console** → **Test** → **MQTT Test Client**
2. Publica en el topic `$aws/things/SignalSpeed_SL_R1s1_001/shadow/update`:
   ```json
   {
     "state": {
       "desired": {
         "activate": true
       }
     }
   }
   ```

#### Monitorear estado de la señal
Suscríbete al topic: `$aws/things/SignalSpeed_SL_R1s1_001/shadow/update/accepted`

Recibirás estados como:
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
      "timestamp": 1702000000000
    }
  }
}
```

## Flujo de funcionamiento

1. **Inicio**: SignalSpeed se conecta al broker MQTT local y a AWS IoT
2. **Suscripción**: La señal se suscribe al topic `step` del simulador
3. **Publicación**: Cuando recibe un paso de simulación, publica su estado:
   - Localmente en el topic MQTT: `es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{roadSegment}/signals`
   - En AWS: Publica en Device Shadow reportado
4. **Recepción de comandos**: Escucha cambios en el estado "desired" del Device Shadow
5. **Control remoto**: Se puede activar/desactivar desde AWS

## Troubleshooting

### Problema: "Error connecting to AWS IoT"
- Verifica que los certificados existan en `certs/`
- Revisa el endpoint en `SignalSpeed_AWSShadowPublisher.java`
- Confirma que el Thing existe en AWS IoT Console

### Problema: "Certificado no encontrado"
- El código intenta usar certificados específicos primero
- Si no existen, usa `dispositivo2-certificate.pem.crt` por defecto
- Verifica permisos de archivo

### Problema: "Conexión local MQTT no funciona"
- Verifica que el broker MQTT local está corriendo en `tambori.dsic.upv.es:10083`
- Revisa que el topic está correcto

## Estructura de Archivos

```
SignalSpeed/
├── src/
│   └── dispositivo/
│       ├── componentes/
│       │   ├── SignalSpeed.java                    // Clase principal
│       │   ├── SignalSpeed_AWSShadowPublisher.java // Publica estado en AWS
│       │   └── SignalSpeed_AWSShadowSubscriber.java // Recibe comandos de AWS
│       ├── iniciador/
│       │   ├── SignalSpeedIniciador.java            // Iniciador básico
│       │   └── SignalSpeedIniciador_AWSShadow.java  // Iniciador con AWS
│       └── utils/
│           └── MySimpleLogger.java                 // Logger
├── certs/
│   ├── dispositivo2-certificate.pem.crt
│   └── dispositivo2-private.pem.key
└── policies/
    └── senial_SL_R1s1_001_policy-v1.json          // Política de permisos AWS
```

## Notas

- La señal publica su estado automáticamente en cada paso de simulación
- Los comandos de activación/desactivación se procesan de forma asincrónica
- El estado se sincroniza entre el simulador local y AWS
- El endpoint de AWS está configurado para la región `us-east-1`

