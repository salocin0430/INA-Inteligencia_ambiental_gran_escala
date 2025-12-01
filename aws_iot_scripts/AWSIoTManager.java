package commons;

import java.io.File;
import java.util.UUID;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import software.amazon.awssdk.iot.iotdevicecommon.internal.mqtt.MqttConnection;
import software.amazon.awssdk.crt.http.HttpRequest;
import org.eclipse.paho.client.mqttv3.*;

public class AWSIoTManager {
    
    private String endpoint;
    private String thingName;
    private String certPath;
    private String keyPath;
    private String caPath;
    private MqttClient awsClient;
    
    public AWSIoTManager(String endpoint, String thingName, 
                        String certPath, String keyPath, String caPath) {
        this.endpoint = endpoint;
        this.thingName = thingName;
        this.certPath = certPath;
        this.keyPath = keyPath;
        this.caPath = caPath;
    }
    
    // Conectar a AWS IoT
    public void connect() throws MqttException {
        String brokerUrl = "ssl://" + endpoint + ":8883";
        awsClient = new MqttClient(brokerUrl, 
                                   "JavaClient_" + UUID.randomUUID().toString());
        
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        
        // Configurar certificados
        try {
            options.setSocketFactory(
                SSLSocketFactory.getSocketFactory(
                    new File(caPath),
                    new File(certPath),
                    new File(keyPath),
                    "password" // si lo necesita
                )
            );
        } catch (Exception e) {
            System.err.println("Error configurando certificados: " + e.getMessage());
        }
        
        awsClient.connect(options);
        System.out.println("✓ Conectado a AWS IoT: " + brokerUrl);
    }
    
    // Publicar en Device Shadow
    public void updateDeviceShadow(String reportedState) throws MqttException {
        String topic = "$aws/things/" + thingName + "/shadow/update";
        String payload = "{\"state\": {\"reported\": " + reportedState + "}}";
        
        awsClient.publish(topic, payload.getBytes(), 1, true);
        System.out.println("✓ Device Shadow actualizado: " + topic);
    }
    
    // Suscribirse a cambios en Device Shadow
    public void subscribeToShadowUpdates(IMqttMessageListener callback) 
            throws MqttException {
        String topic = "$aws/things/" + thingName + "/shadow/update/delta";
        awsClient.subscribe(topic, 1, callback);
        System.out.println("✓ Suscrito a cambios de shadow: " + topic);
    }
    
    // Publicar mensaje MQTT
    public void publish(String topic, String payload) throws MqttException {
        awsClient.publish(topic, payload.getBytes(), 1, true);
        System.out.println("✓ Publicado en " + topic);
    }
    
    // Suscribirse a topic
    public void subscribe(String topic, IMqttMessageListener callback) 
            throws MqttException {
        awsClient.subscribe(topic, 1, callback);
        System.out.println("✓ Suscrito a: " + topic);
    }
    
    public void disconnect() throws MqttException {
        if (awsClient != null && awsClient.isConnected()) {
            awsClient.disconnect();
            System.out.println("✓ Desconectado de AWS IoT");
        }
    }
}
