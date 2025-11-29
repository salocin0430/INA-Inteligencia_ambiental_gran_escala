# Guía de Configuración AWS IoT

## Pasos para configurar tu cuenta AWS IoT

### 1. Obtener el Endpoint de AWS IoT

1. Ve a la **Consola AWS IoT** → **Configuración** (Settings)
2. Copia el **Endpoint** (formato: `xxxxx-ats.iot.us-east-1.amazonaws.com`)
3. Actualiza la variable `clientEndpoint` en `AWSIoTThingConnectionStarter.java`

**Endpoint actual configurado:** `a1knxlrh9s811y-ats.iot.us-east-1.amazonaws.com`

### 2. Descargar Certificados del Dispositivo

1. Ve a **Dispositivos/Objetos** → Selecciona tu dispositivo
2. Ve a la pestaña **Seguridad** → **Certificados**
3. Descarga los siguientes archivos:
   - **Certificate** (`.pem.crt`) - Ejemplo: `abc123-certificate.pem.crt`
   - **Private key** (`.pem.key`) - Ejemplo: `abc123-private.pem.key`
4. **Guarda estos archivos en la carpeta `certs/`**

### 3. Actualizar Nombres de Certificados en el Código

Edita `AWSIoTThingConnectionStarter.java` y reemplaza:
- `TU_CERTIFICADO-certificate.pem.crt` → Nombre real de tu certificado
- `TU_CERTIFICADO-private.pem.key` → Nombre real de tu clave privada

**Ejemplo:**
Si descargaste `ttmi052-certificate.pem.crt` y `ttmi052-private.pem.key`:
```java
protected static String certificateFile = certsDir + "ttmi052-certificate.pem.crt";
protected static String privateKeyFile = certsDir + "ttmi052-private.pem.key";
```

### 4. Verificar Políticas de Seguridad

Asegúrate de que tu certificado tenga una política que permita:
- `iot:Connect` - Para conectarse
- `iot:Publish` - Para publicar mensajes
- `iot:Subscribe` - Para suscribirse a topics
- `iot:Receive` - Para recibir mensajes

### 5. Probar la Conexión

Ejecuta el programa con:
```bash
java -jar awsiotthing.jar clientId=mi_test_001
```

O desde VSCode, configura los argumentos en `launch.json`:
```json
{
    "type": "java",
    "name": "AWS IoT Test",
    "request": "launch",
    "mainClass": "aws.iot.connection.AWSIoTThingConnectionStarter",
    "args": ["clientId=mi_test_001"]
}
```

## Estructura de Archivos Esperada

```
seminario-4-dispositivo-iot-aws/
├── certs/
│   ├── TU_CERTIFICADO-certificate.pem.crt  ← Tu certificado aquí
│   └── TU_CERTIFICADO-private.pem.key      ← Tu clave privada aquí
└── dispositivo-aws/
    └── src/
        └── aws/iot/connection/
            └── AWSIoTThingConnectionStarter.java
```

## Notas Importantes

⚠️ **NUNCA subas los certificados a Git** - Añade `certs/*.pem.*` a `.gitignore`

⚠️ **Los certificados son únicos** - Si los pierdes, debes generar nuevos en AWS IoT

⚠️ **El endpoint es específico de tu cuenta** - Cada cuenta AWS tiene su propio endpoint

