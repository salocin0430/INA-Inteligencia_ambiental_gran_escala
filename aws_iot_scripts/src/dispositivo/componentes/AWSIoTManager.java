package dispositivo.componentes;

import java.io.File;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import com.amazonaws.services.iot.client.*;
import com.amazonaws.services.iot.client.util.AwsIotTlsSocketFactory;
import dispositivo.utils.MySimpleLogger;

public class AWSIoTManager {
    
    private String endpoint;
    private String thingName;
    private String certPath;
    private String keyPath;
    private String caPath;
    private MqttClient awsClient; // Mantener para compatibilidad
    private AWSIotMqttClient awsIotClient; // Nuevo cliente AWS SDK
    private String loggerId;
    private boolean useAwsSdk = true; // Usar AWS SDK por defecto
    
    public AWSIoTManager(String endpoint, String thingName, 
                        String certPath, String keyPath, String caPath) {
        this.endpoint = endpoint;
        this.thingName = thingName;
        this.certPath = certPath;
        this.keyPath = keyPath;
        this.caPath = caPath;
        this.loggerId = "AWSIoTManager-" + thingName;
    }
    
    // Conectar a AWS IoT usando AWS IoT Device SDK (soporta PEM)
    public void connect() throws MqttException {
        try {
            // Verificar que los certificados existen
            File certFile = new File(certPath);
            File keyFile = new File(keyPath);
            File caFile = new File(caPath);
            
            if (!certFile.exists() || !keyFile.exists() || !caFile.exists()) {
                String errorMsg = "Certificados no encontrados. AWS IoT no estará disponible.\n" +
                    "  Cert: " + certPath + " existe: " + certFile.exists() + "\n" +
                    "  Key: " + keyPath + " existe: " + keyFile.exists() + "\n" +
                    "  CA: " + caPath + " existe: " + caFile.exists();
                MySimpleLogger.error(loggerId, errorMsg);
                throw new MqttException(new Exception("Certificados no encontrados"));
            }
            
            // Usar AWS IoT Device SDK que soporta certificados PEM
            try {
                MySimpleLogger.info(loggerId, "Intentando conectar usando AWS IoT Device SDK (soporta PEM)...");
                
                // Configurar el cliente AWS IoT
                AWSIotConfig config = new AWSIotConfig();
                config.setClientEndpoint(endpoint);
                config.setClientId("JavaClient_" + UUID.randomUUID().toString());
                config.setCertificateFilePath(certPath);
                config.setPrivateKeyFilePath(keyPath);
                config.setCaFilePath(caPath);
                config.setKeepAliveInterval(60);
                config.setConnectionTimeout(30);
                config.setCleanSession(true);
                
                // Crear cliente AWS IoT
                awsIotClient = new AWSIotMqttClient(config);
                
                // Conectar
                awsIotClient.connect();
                
                MySimpleLogger.info(loggerId, "✓ Conectado a AWS IoT usando Device SDK: " + endpoint);
                useAwsSdk = true;
                
            } catch (Exception awsEx) {
                MySimpleLogger.warn(loggerId, "Error usando AWS SDK: " + awsEx.getMessage());
                MySimpleLogger.warn(loggerId, "Intentando fallback con Paho MQTT (puede fallar sin certificados configurados)...");
                
                // Fallback a Paho (no funcionará sin conversión de certificados)
                String brokerUrl = "ssl://" + endpoint + ":8883";
                String clientId = "JavaClient_" + UUID.randomUUID().toString();
                awsClient = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
                
                MqttConnectOptions options = new MqttConnectOptions();
                options.setCleanSession(true);
                options.setKeepAliveInterval(60);
                options.setConnectionTimeout(30);
                
                awsClient.connect(options);
                useAwsSdk = false;
                MySimpleLogger.info(loggerId, "✓ Conectado a AWS IoT usando Paho: " + brokerUrl);
            }
            
        } catch (Exception e) {
            String errorMsg = "Error conectando a AWS IoT: " + e.getMessage();
            if (e.getCause() != null) {
                errorMsg += " (Causa: " + e.getCause().getMessage() + ")";
            }
            MySimpleLogger.error(loggerId, errorMsg);
            MySimpleLogger.warn(loggerId, "El programa continuará sin conexión AWS. Funcionalidad local MQTT seguirá activa.");
            throw new MqttException(e);
        }
    }
    
    // Publicar en Device Shadow
    public void updateDeviceShadow(String reportedState) throws MqttException {
        String topic = "$aws/things/" + thingName + "/shadow/update";
        String payload = "{\"state\": {\"reported\": " + reportedState + "}}";
        publish(topic, payload);
    }
    
    // Publicar mensaje MQTT
    public void publish(String topic, String payload) throws MqttException {
        if (useAwsSdk && awsIotClient != null && awsIotClient.isConnected()) {
            try {
                AWSIotMessage message = new AWSIotMessage(topic, AWSIotQos.QOS1, payload);
                awsIotClient.publish(message);
                MySimpleLogger.info(loggerId, "✓ Publicado en " + topic + " (AWS SDK)");
            } catch (AWSIotException e) {
                MySimpleLogger.error(loggerId, "Error publicando en AWS SDK: " + e.getMessage());
                throw new MqttException(e);
            }
        } else if (awsClient != null && awsClient.isConnected()) {
            try {
                awsClient.publish(topic, payload.getBytes(), 1, true);
                MySimpleLogger.info(loggerId, "✓ Publicado en " + topic + " (Paho)");
            } catch (MqttException e) {
                MySimpleLogger.error(loggerId, "Error publicando en AWS: " + e.getMessage());
                throw e;
            }
        } else {
            MySimpleLogger.warn(loggerId, "Cliente AWS no conectado, no se puede publicar en: " + topic);
        }
    }
    
    // Suscribirse a topic
    public void subscribe(String topic, int qos) throws MqttException {
        if (useAwsSdk && awsIotClient != null && awsIotClient.isConnected()) {
            try {
                AWSIotQos awsQos = (qos == 0) ? AWSIotQos.QOS0 : AWSIotQos.QOS1;
                awsIotClient.subscribe(new AWSIotTopic(topic, awsQos));
                MySimpleLogger.info(loggerId, "✓ Suscrito a: " + topic + " (AWS SDK)");
            } catch (AWSIotException e) {
                MySimpleLogger.error(loggerId, "Error suscribiéndose en AWS SDK: " + e.getMessage());
                throw new MqttException(e);
            }
        } else if (awsClient != null && awsClient.isConnected()) {
            try {
                awsClient.subscribe(topic, qos);
                MySimpleLogger.info(loggerId, "✓ Suscrito a: " + topic + " (Paho)");
            } catch (MqttException e) {
                MySimpleLogger.error(loggerId, "Error suscribiéndose: " + e.getMessage());
                throw e;
            }
        } else {
            MySimpleLogger.warn(loggerId, "Cliente AWS no conectado, no se puede suscribir a: " + topic);
        }
    }
    
    // Configurar callback para recibir mensajes (solo para Paho)
    public void setCallback(MqttCallback callback) {
        if (awsClient != null) {
            awsClient.setCallback(callback);
        } else if (awsIotClient != null) {
            // Para AWS SDK, el callback se configura en el subscribe usando AWSIotTopic
            MySimpleLogger.warn(loggerId, "Para AWS SDK, usa subscribe con AWSIotTopic que incluye callback");
        }
    }
    
    // Suscribirse con callback (para AWS SDK)
    public void subscribeWithCallback(String topic, int qos, AWSIotTopicCallback callback) throws MqttException {
        if (useAwsSdk && awsIotClient != null && awsIotClient.isConnected()) {
            try {
                AWSIotQos awsQos = (qos == 0) ? AWSIotQos.QOS0 : AWSIotQos.QOS1;
                AWSIotTopic awsTopic = new AWSIotTopic(topic, awsQos, callback);
                awsIotClient.subscribe(awsTopic);
                MySimpleLogger.info(loggerId, "✓ Suscrito a: " + topic + " con callback (AWS SDK)");
            } catch (AWSIotException e) {
                MySimpleLogger.error(loggerId, "Error suscribiéndose con callback: " + e.getMessage());
                throw new MqttException(e);
            }
        } else {
            subscribe(topic, qos);
        }
    }
    
    public MqttClient getClient() {
        return awsClient;
    }
    
    public AWSIotMqttClient getAwsIotClient() {
        return awsIotClient;
    }
    
    public void disconnect() throws MqttException {
        if (useAwsSdk && awsIotClient != null && awsIotClient.isConnected()) {
            try {
                awsIotClient.disconnect();
                MySimpleLogger.info(loggerId, "✓ Desconectado de AWS IoT (AWS SDK)");
            } catch (AWSIotException e) {
                MySimpleLogger.error(loggerId, "Error desconectando AWS SDK: " + e.getMessage());
                throw new MqttException(e);
            }
        } else if (awsClient != null && awsClient.isConnected()) {
            try {
                awsClient.disconnect();
                MySimpleLogger.info(loggerId, "✓ Desconectado de AWS IoT (Paho)");
            } catch (MqttException e) {
                MySimpleLogger.error(loggerId, "Error desconectando: " + e.getMessage());
                throw e;
            }
        }
    }
    
    public boolean isConnected() {
        if (useAwsSdk && awsIotClient != null) {
            return awsIotClient.isConnected();
        } else if (awsClient != null) {
            return awsClient.isConnected();
        }
        return false;
    }
}
