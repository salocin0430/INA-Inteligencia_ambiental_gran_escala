# Solución de Problemas - AWS IoT Connection

## Error: Connection lost (32109) - java.io.EOFException

Este error generalmente indica un problema de **autenticación o autorización**. Sigue estos pasos:

### 1. Verificar que la Política esté Creada en AWS

1. Ve a **AWS IoT Console** → **Seguridad** → **Políticas**
2. Busca la política `dispositivo2-basic-policy`
3. Si NO existe, créala siguiendo los pasos en `COMO_OBTENER_ACCOUNT_ID.md`

### 2. Verificar que la Política esté Asociada al Certificado

1. Ve a **Seguridad** → **Certificados**
2. Selecciona tu certificado (`dispositivo2`)
3. Abre la pestaña **Políticas**
4. Verifica que `dispositivo2-basic-policy` esté listada
5. Si NO está, haz clic en **Adjuntar políticas** y selecciónala

### 3. Verificar el Client ID

El `clientId` debe empezar con `dispositivo2-` para coincidir con la política.

**Política permite:**
```json
"Resource": "arn:aws:iot:us-east-1:931743417574:client/dispositivo2-*"
```

**Código genera:**
```java
clientId = "dispositivo2-" + UUID.randomUUID().toString();
```

✅ Esto ya está corregido en el código.

### 4. Verificar los Certificados

Los certificados deben estar en la carpeta `certs/`:
- `dispositivo2-certificate.pem.crt`
- `dispositivo2-private.pem.key`

✅ Los archivos existen según la verificación.

### 5. Verificar el Endpoint

El endpoint debe ser: `a1knxlrh9s811y-ats.iot.us-east-1.amazonaws.com`

✅ Ya está configurado correctamente.

### 6. Verificar la Región en la Política

La política usa `us-east-1`. Si tu cuenta está en otra región, actualiza la política.

### 7. Verificar que el Certificado esté Activo

1. Ve a **Seguridad** → **Certificados**
2. Selecciona tu certificado
3. Verifica que el estado sea **ACTIVO**
4. Si está **INACTIVO**, haz clic en **Activar**

### 8. Verificar los Permisos de la Política

Asegúrate de que la política tenga TODOS estos permisos:

- ✅ `iot:Connect` - Para conectarse
- ✅ `iot:Publish` - Para publicar (si vas a publicar)
- ✅ `iot:Subscribe` - Para suscribirse (si vas a suscribirte)
- ✅ `iot:Receive` - Para recibir mensajes (si vas a recibir)

### 9. Probar con el Cliente de Prueba MQTT de AWS

1. Ve a **Probar** → **Cliente de prueba de MQTT**
2. Intenta conectarte desde ahí
3. Si funciona ahí pero no desde tu código, el problema está en la configuración del código
4. Si NO funciona ni desde ahí, el problema está en la política/certificado

### 10. Verificar los Logs Detallados

Ejecuta el programa y revisa:
- ¿Se conecta inicialmente? (debería ver "Client Connected to AWS IoT MQTT")
- ¿Falla inmediatamente después?
- ¿Qué mensaje de error exacto aparece?

## Checklist Rápido

- [ ] Política `dispositivo2-basic-policy` creada en AWS
- [ ] Política asociada al certificado `dispositivo2`
- [ ] Certificado está ACTIVO
- [ ] Client ID empieza con `dispositivo2-`
- [ ] Endpoint correcto
- [ ] Certificados en la carpeta `certs/`
- [ ] Account ID correcto en la política (931743417574)

## Si Nada Funciona

1. Crea una política más permisiva temporalmente para probar:
   - Permite `iot:Connect` con `*` (cualquier cliente)
   - Permite todos los topics con `*`
2. Si funciona con la política permisiva, el problema es la configuración específica
3. Luego restringe la política gradualmente

