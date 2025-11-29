# Integración del Navigator con SmartCar

## ✅ Lo que hemos implementado

### 1. **SmartCar_StepSubscriber.java** (NUEVO)
- Se suscribe al topic `es/upv/pros/tatami/smartcities/traffic/PTPaterna/step`
- Cada vez que recibe un paso de simulación (cada 3 segundos), llama a `smartcar.onSimulationStep()`

### 2. **SmartCar_TrafficPublisher.java** (ACTUALIZADO)
- Actualizado para publicar en el formato del Proyecto Práctico:
  - `publishVehicleIn(roadSegment, position)` - Formato VEHICLE_IN
  - `publishVehicleOut(roadSegment, position)` - Formato VEHICLE_OUT
- Formato del mensaje según documento:
  ```json
  {
    "msg": {
      "action": "VEHICLE_IN",
      "vehicle-role": "PrivateUsage",
      "vehicle-id": "SmartCar001",
      "road-segment": "R5s1",
      "position": 100
    },
    "id": "MSG_1638979846783",
    "type": "TRAFFIC",
    "timestamp": 1638979846783
  }
  ```

### 3. **SmartCar.java** (EXTENDIDO)
- ✅ Integrado Navigator
- ✅ Suscripción automática al topic 'step'
- ✅ Método `onSimulationStep()` que mueve el Navigator en cada paso
- ✅ Detección automática de cambios de segmento
- ✅ Publicación automática de VEHICLE_IN/VEHICLE_OUT
- ✅ Soporte para vehículos especiales (Ambulance, Police)
- ✅ Velocidad de crucero configurable
- ✅ Método `setRoute()` para asignar rutas

## 📋 Configuración necesaria

### 1. Agregar Navigator Component al proyecto

Necesitas agregar las clases compiladas del Navigator Component al classpath:

**Opción A: Copiar los .jar o .class compilados**
```
seminario3-smarttraffic/Paso1/smartcar/lib/
  - java-json.jar (ya existe)
  - org.eclipse.paho.client.mqttv3_1.0.2.jar (ya existe)
  - ina.vehicle.navigation.components.jar (NUEVO - necesitas compilarlo)
```

**Opción B: Agregar como proyecto referenciado**
- En tu IDE, agrega `INA-2022.Navigator.Component/ina.vehicle.navigation.components` como proyecto referenciado

**Opción C: Compilar y copiar manualmente**
```bash
# Compilar el Navigator Component
cd INA-2022.Navigator.Component/ina.vehicle.navigation.components
javac -d bin src/ina/vehicle/navigation/**/*.java

# Copiar los .class a smartcar/lib o agregar al classpath
```

### 2. Verificar imports

Los imports en `SmartCar.java` son:
```java
import ina.vehicle.navigation.components.Navigator;
import ina.vehicle.navigation.components.Route;
import ina.vehicle.navigation.interfaces.INavigator;
import ina.vehicle.navigation.interfaces.IRoadPoint;
import ina.vehicle.navigation.interfaces.IRoute;
```

## 🚀 Cómo usar

### Ejemplo básico:

```java
// 1. Crear SmartCar
SmartCar vehiculo = new SmartCar("Car-001");
vehiculo.setVehicleRole("PrivateUsage");
vehiculo.setCruiserSpeed(60);

// 2. Crear ruta
Route ruta = new Route();
ruta.addRouteFragment("R5s1", 0, 300);

// 3. Asignar ruta (esto inicia el Navigator automáticamente)
vehiculo.setRoute(ruta);

// 4. El vehículo se moverá automáticamente en cada paso de simulación
// No necesitas hacer nada más, el StepSubscriber se encarga
```

### Ejemplo con vehículo especial (Ambulancia):

```java
// Crear ambulancia
SmartCar ambulancia = new SmartCar("Ambulance-001");
ambulancia.setVehicleRole("Ambulance");
ambulancia.setCruiserSpeed(80);
ambulancia.setIgnoreLimits(true); // Ignora límites de velocidad

// Crear ruta desde hospital hasta accidente
Route ruta = new Route();
ruta.addRouteFragment("R9s1", 0, 415);  // Desde hospital
ruta.addRouteFragment("R5s1", 0, 100);   // Hasta accidente

ambulancia.setRoute(ruta);
```

## 🔄 Flujo de funcionamiento

```
1. SmartCar se crea y se suscribe al topic 'step'
   ↓
2. Simulador publica mensaje 'step' (cada 3 segundos)
   ↓
3. SmartCar_StepSubscriber recibe el mensaje
   ↓
4. Llama a smartcar.onSimulationStep()
   ↓
5. SmartCar calcula velocidad actual
   ↓
6. Navigator.move(3000, velocidad) - calcula nueva posición
   ↓
7. SmartCar detecta si cambió de segmento
   ↓
8. Publica VEHICLE_OUT (si cambió) y VEHICLE_IN (siempre)
   ↓
9. El simulador escucha y actualiza num-vehicles
```

## 📝 Métodos principales

### SmartCar
- `setRoute(IRoute route)` - Asigna ruta e inicia Navigator
- `setVehicleRole(String role)` - "PrivateUsage", "Ambulance", "Police"
- `setCruiserSpeed(int speed)` - Velocidad de crucero en km/h
- `setIgnoreLimits(boolean ignore)` - Para vehículos especiales
- `onSimulationStep()` - Llamado automáticamente en cada paso
- `disconnect()` - Desconecta todos los componentes MQTT

### SmartCar_TrafficPublisher
- `publishVehicleIn(roadSegment, position)` - Publica entrada en segmento
- `publishVehicleOut(roadSegment, position)` - Publica salida de segmento

## ⚠️ Pendiente de implementar

1. **Cálculo de velocidad dinámica** (`calcularVelocidadActual()`):
   - Consultar límite de velocidad del segmento (vía REST o MQTT)
   - Consultar señales de tráfico (speed-limit, semáforos)
   - Retornar mínimo entre: límite, señal, velocidad crucero

2. **Suscripción a señales de tráfico**:
   - Suscribirse al topic `.../road/{segment}/signals`
   - Procesar mensajes de tipo TRAFFIC_SIGNAL
   - Ajustar velocidad según señales

3. **Suscripción a información de segmentos**:
   - Ya está implementado en `SmartCar_RoadInfoSubscriber`
   - Procesar mensajes ROAD_STATUS para obtener límites de velocidad

## 🧪 Pruebas

Para probar:

1. **Compilar el proyecto** (asegúrate de tener Navigator en el classpath)
2. **Ejecutar SmartCarStarter_Navigator**
3. **Verificar en MQTT.fx** que se publican mensajes en:
   - `.../road/R5s1/traffic` (VEHICLE_IN)
   - `.../step` (debe recibir pasos del simulador)
4. **Consultar REST API**:
   - `GET http://tambori.dsic.upv.es:10082/segment/R5s1`
   - Verificar que `num-vehicles` aumenta cuando publicas VEHICLE_IN

