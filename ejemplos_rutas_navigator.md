# Ejemplos de Rutas con Navigator Component

## Cómo interpretar el JSON para crear rutas

### Estructura de un segmento:
```json
{
  "code": "R1s1",           // ID del segmento
  "start-kp": 0,            // Punto kilométrico inicial
  "end-kp": 50,             // Punto kilométrico final
  "length": 50,             // Longitud en metros
  "max-speed": 40           // Velocidad máxima
}
```

## Ejemplo 1: Ruta simple en R5s1

**Objetivo:** Ir del inicio al final de R5s1

```java
IRoute ruta = new Route();
// R5s1: start-kp=0, end-kp=580
ruta.addRouteFragment("R5s1", 0, 580);

navigator.setRoute(ruta);
navigator.startRouting();
```

## Ejemplo 2: Ruta en R1 (múltiples segmentos)

**Objetivo:** Ir de R1s1 (inicio) hasta R1s2a (final)

Mirando el JSON:
- R1s1: start-kp=0, end-kp=50
- R1s2a: start-kp=20, end-kp=320

**IMPORTANTE:** R1s1 termina en 50, pero R1s2a empieza en 20. 
Esto significa que hay una intersección entre ellos.

```java
IRoute ruta = new Route();
// Recorrer todo R1s1
ruta.addRouteFragment("R1s1", 0, 50);
// Continuar en R1s2a desde el punto de conexión (20)
ruta.addRouteFragment("R1s2a", 20, 320);

navigator.setRoute(ruta);
navigator.startRouting();
```

## Ejemplo 3: Ruta del Proyecto - De R5s1 a R1s4a

**Objetivo:** Ir desde R5s1 punto 100 hasta R1s4a punto 600

```java
IRoute ruta = new Route();
// Empezar en R5s1, punto 100 (no desde 0)
ruta.addRouteFragment("R5s1", 100, 580);  // Ir hasta el final de R5s1
// Continuar en R1s4a (que empieza en 340 según el JSON)
ruta.addRouteFragment("R1s4a", 340, 600);

navigator.setRoute(ruta);
navigator.startRouting();
```

## Ejemplo 4: Ruta para Ambulancia (Proyecto Práctico)

**Objetivo:** Ir desde "Hospital" (ej: R9s1 punto 0) hasta accidente en R5s1 punto 100

```java
IRoute rutaAmbulancia = new Route();
// Desde el hospital (R9s1)
rutaAmbulancia.addRouteFragment("R9s1", 0, 415);
// Continuar hacia R5s1 (necesitas encontrar la conexión)
// ... (depende de cómo se conecten en el mapa)
rutaAmbulancia.addRouteFragment("R5s1", 0, 100);  // Hasta el accidente

navigator.setRoute(rutaAmbulancia);
navigator.startRouting();
```

## Reglas importantes:

1. **Puntos de conexión:** Cuando un segmento termina y otro empieza, 
   los puntos kilométricos pueden no coincidir exactamente.
   Busca las intersecciones en el mapa.

2. **Segmentos bidireccionales:** 
   - "a" = ascendente (dirección normal)
   - "d" = descendente (dirección contraria)
   - Ejemplo: R1s2a (0→320) vs R1s2d (320→0)

3. **Rangos válidos:** 
   - El punto inicial debe estar entre start-kp y end-kp
   - El punto final debe estar entre start-kp y end-kp
   - Ejemplo: R5s1 (0-580) → puedes usar cualquier punto entre 0 y 580

4. **Orden de los fragmentos:**
   - El final de un fragmento debe conectarse con el inicio del siguiente
   - El Navigator calcula automáticamente el movimiento entre segmentos

## Cómo encontrar conexiones entre segmentos:

1. **Consulta el mapa visual** para ver qué segmentos se conectan
2. **Consulta el API REST:**
   ```
   GET http://tambori.dsic.upv.es:10082/segment/R1s1
   ```
   Puede devolver información sobre conexiones

3. **Observa los puntos kilométricos:**
   - Si R1s1 termina en 50 y R1s2a empieza en 20, 
     significa que hay una intersección en algún punto intermedio

## Ejemplo completo con código:

```java
import ina.vehicle.navigation.components.Navigator;
import ina.vehicle.navigation.components.Route;
import ina.vehicle.navigation.interfaces.INavigator;
import ina.vehicle.navigation.interfaces.IRoute;

public class EjemploRuta {
    public static void main(String[] args) {
        INavigator navigator = new Navigator("mi-vehiculo");
        
        // Crear ruta: R5s1 desde punto 100 hasta 300
        IRoute ruta = new Route();
        ruta.addRouteFragment("R5s1", 100, 300);
        
        // Asignar ruta
        navigator.setRoute(ruta);
        
        // Iniciar navegación
        navigator.startRouting();
        
        // En cada paso de simulación (cada 3 segundos):
        int velocidad = 60; // km/h
        navigator.move(3000, velocidad); // 3 segundos = 3000 ms
        
        // Consultar posición actual
        System.out.println("Posición: " + navigator.getCurrentPosition());
    }
}
```

