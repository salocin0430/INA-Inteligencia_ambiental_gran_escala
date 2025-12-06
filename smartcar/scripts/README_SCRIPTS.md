# Scripts de Ejecución para Pruebas

Este directorio contiene scripts para ejecutar diferentes tipos de vehículos desde binarios compilados.

## Estructura de Scripts

```
smartcar/
├── compilar.sh                    # Script para compilar sin ejecutar
├── compilar_y_ejecutar.sh         # Script original (compila y ejecuta)
└── scripts/
    ├── ejecutar_vehiculo_normal.sh      # Ejecuta vehículo normal
    ├── ejecutar_vehiculo_policia.sh     # Ejecuta vehículo policía
    ├── ejecutar_vehiculo_ambulancia.sh  # Ejecuta ambulancia
    ├── ejecutar_todos_vehiculos.sh      # Ejecuta todos en terminales separadas
    └── README_SCRIPTS.md                # Este archivo
```

## Requisitos Previos

1. **Compilar el proyecto primero:**
   ```bash
   cd ..
   ./compilar.sh
   # O usar: ./compilar_y_ejecutar.sh (y presionar Ctrl+C después de compilar)
   ```

2. **Certificados AWS IoT:**
   - Los certificados deben estar en `../certs/`
   - Formato: `{thingName}-certificate.pem.crt` y `{thingName}-private.pem.key`

## Flujo de Trabajo

### 1. Compilar el Proyecto

```bash
cd seminario3-smarttraffic/Paso1/smartcar
./compilar.sh
```

Esto genera los binarios en `bin/` sin ejecutar nada.

### 2. Ejecutar Vehículos

Puedes ejecutar vehículos individuales o todos simultáneamente (ver secciones siguientes).

## Scripts Disponibles

### 1. ejecutar_vehiculo_normal.sh

Ejecuta un vehículo normal (PrivateUsage).

**Uso:**
```bash
./ejecutar_vehiculo_normal.sh [id] [thingName] [velocidad] [ruta]
```

**Parámetros (todos opcionales):**
- `id`: ID del vehículo (default: SmartCar001)
- `thingName`: Nombre del Thing en AWS IoT (default: SmartCar097)
- `velocidad`: Velocidad de crucero en km/h (default: 60)
- `ruta`: Ruta en formato `segmento1:inicio:fin,segmento2:inicio:fin` (default: R5s1:0:580,R1s4a:490:600)

**Ejemplos:**
```bash
# Con valores por defecto
./ejecutar_vehiculo_normal.sh

# Especificando todos los parámetros
./ejecutar_vehiculo_normal.sh SmartCar002 SmartCar097 70 "R2s1:0:300,R2s2:300:750"
```

---

### 2. ejecutar_vehiculo_policia.sh

Ejecuta un vehículo de policía (ignora límites de velocidad y semáforos).

**Uso:**
```bash
./ejecutar_vehiculo_policia.sh [id] [thingName] [velocidad] [ruta]
```

**Parámetros (todos opcionales):**
- `id`: ID del vehículo (default: PoliceCar001)
- `thingName`: Nombre del Thing en AWS IoT (default: SmartCar097)
- `velocidad`: Velocidad de crucero en km/h (default: 80)
- `ruta`: Ruta (default: R2s1:0:300,R2s2:300:750)

**Ejemplos:**
```bash
# Con valores por defecto
./ejecutar_vehiculo_policia.sh

# Especificando parámetros
./ejecutar_vehiculo_policia.sh PoliceCar002 SmartCar097 100 "R5s1:0:580"
```

---

### 3. ejecutar_vehiculo_ambulancia.sh

Ejecuta una ambulancia (ignora límites de velocidad y semáforos).

**Uso:**
```bash
./ejecutar_vehiculo_ambulancia.sh [id] [thingName] [velocidad] [ruta]
```

**Parámetros (todos opcionales):**
- `id`: ID del vehículo (default: Ambulance001)
- `thingName`: Nombre del Thing en AWS IoT (default: SmartCar097)
- `velocidad`: Velocidad de crucero en km/h (default: 90)
- `ruta`: Ruta (default: R1s1:0:50,R1s2:0:100)

**Ejemplos:**
```bash
# Con valores por defecto
./ejecutar_vehiculo_ambulancia.sh

# Especificando parámetros
./ejecutar_vehiculo_ambulancia.sh Ambulance002 SmartCar097 110 "R5s1:100:580"
```

---

### 4. ejecutar_todos_vehiculos.sh

Ejecuta todos los vehículos simultáneamente en terminales separadas.

**Uso:**
```bash
./ejecutar_todos_vehiculos.sh
```

Este script abre 3 terminales separadas, una para cada tipo de vehículo:
- Terminal 1: Vehículo Normal
- Terminal 2: Vehículo Policía
- Terminal 3: Ambulancia

---

## Formato de Rutas

Las rutas se especifican en formato:
```
segmento1:inicio:fin,segmento2:inicio:fin,segmento3:inicio:fin
```

**Ejemplos de rutas:**

1. **Ruta simple (un solo segmento):**
   ```
   R5s1:0:580
   ```

2. **Ruta con cambio de segmento:**
   ```
   R5s1:0:580,R1s4a:490:600
   ```

3. **Ruta en R2 (segmentos conectados):**
   ```
   R2s1:0:300,R2s2:300:750
   ```

4. **Ruta corta:**
   ```
   R1s1:0:50,R1s2:0:100
   ```

## Casos de Prueba Predefinidos

### Caso 1: Vehículo Normal (Ruta por defecto)
```bash
./ejecutar_vehiculo_normal.sh
```
- ID: SmartCar001
- Ruta: R5s1:0:580 → R1s4a:490:600
- Velocidad: 60 km/h

### Caso 2: Vehículo de Policía (Ruta R2)
```bash
./ejecutar_vehiculo_policia.sh
```
- ID: PoliceCar001
- Ruta: R2s1:0:300 → R2s2:300:750
- Velocidad: 80 km/h (ignora límites)

### Caso 3: Ambulancia (Ruta corta R1)
```bash
./ejecutar_vehiculo_ambulancia.sh
```
- ID: Ambulance001
- Ruta: R1s1:0:50 → R1s2:0:100
- Velocidad: 90 km/h (ignora límites)

### Caso 4: Múltiples Vehículos Simultáneos

**Opción A - Manual (terminales separadas):**

**Terminal 1:**
```bash
./ejecutar_vehiculo_normal.sh SmartCar001 SmartCar097 60 "R5s1:0:580"
```

**Terminal 2:**
```bash
./ejecutar_vehiculo_policia.sh PoliceCar001 SmartCar097 80 "R2s1:0:300"
```

**Terminal 3:**
```bash
./ejecutar_vehiculo_ambulancia.sh Ambulance001 SmartCar097 90 "R1s1:0:50"
```

**Opción B - Automático:**
```bash
./ejecutar_todos_vehiculos.sh
```

## Parámetros de los Scripts

Todos los scripts aceptan los mismos parámetros (todos opcionales):

1. **ID del vehículo** (default según tipo)
2. **Thing Name** (default: SmartCar097)
3. **Velocidad** (default según tipo)
4. **Ruta** (default según tipo)

## Clase Starter Parametrizable

Los scripts utilizan internamente `SmartCarStarter_Test.java` que acepta parámetros por línea de comandos:

```bash
java -cp "$CLASSPATH" smartcar.starter.SmartCarStarter_Test <tipo> <id> <thingName> <velocidad> [ruta]
```

Los scripts de shell son wrappers que facilitan el uso de esta clase.

## Notas Importantes

1. **Compilación:** Asegúrate de compilar antes de ejecutar:
   ```bash
   cd ..
   ./compilar.sh
   ```

2. **Certificados:** Cada vehículo necesita certificados AWS IoT. Si usas el mismo Thing para todos, comparten certificados.

3. **Rutas válidas:** Verifica que los segmentos y posiciones existan en el simulador antes de ejecutar.

4. **Vehículos especiales:** Los vehículos de policía y ambulancia ignoran límites de velocidad y semáforos en rojo.

5. **AWS IoT:** Todos los vehículos publican su estado en AWS IoT Device Shadow y pueden recibir comandos para cambiar velocidad.

6. **Simulador:** El simulador MQTT debe estar corriendo para que los vehículos funcionen correctamente.
