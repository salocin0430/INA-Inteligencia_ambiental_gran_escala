# Cómo Obtener tu Account ID de AWS

## Método 1: Desde la Consola AWS IoT (Más Fácil)

1. Ve a **AWS IoT Console** → **Configuración** (Settings)
2. En la sección **Información**, busca el **ARN** del endpoint
3. El Account ID es el número que aparece en el ARN después de la región

**Ejemplo:**
- Si tu endpoint es: `a1knxlrh9s811y-ats.iot.us-east-1.amazonaws.com`
- El ARN será algo como: `arn:aws:iot:us-east-1:123456789012:...`
- Tu Account ID es: `123456789012`

## Método 2: Desde el Cliente de Prueba MQTT

1. Ve a **AWS IoT Console** → **Probar** → **Cliente de prueba de MQTT**
2. En "Detalles de la conexión", el endpoint muestra tu región
3. El Account ID lo puedes obtener de cualquier ARN en las políticas existentes o desde la configuración

## Método 3: Desde la URL de la Consola

Cuando estás en la consola AWS, la URL puede contener tu Account ID. Pero el método más confiable es el Método 1.

## Método 4: Desde AWS CLI (si lo tienes instalado)

```bash
aws sts get-caller-identity --query Account --output text
```

---

## Después de Obtener tu Account ID

1. Reemplaza `TU_ACCOUNT_ID` en los archivos de políticas:
   - `dispositivo2_basic_policy.json`
   - `dispositivo2_shadow_policy.json`

2. Ve a **AWS IoT Console** → **Seguridad** → **Políticas**

3. Crea una nueva política:
   - Haz clic en **Crear política**
   - Selecciona **JSON**
   - Copia y pega el contenido de una de las políticas (después de reemplazar TU_ACCOUNT_ID)
   - Dale un nombre (ej: `dispositivo2-basic-policy`)
   - Haz clic en **Crear**

4. Asocia la política a tu certificado:
   - Ve a **Seguridad** → **Certificados**
   - Selecciona tu certificado (dispositivo2)
   - Haz clic en **Políticas**
   - Haz clic en **Adjuntar políticas**
   - Selecciona la política que acabas de crear
   - Haz clic en **Adjuntar**

