package dispositivo.componentes;

import dispositivo.utils.MySimpleLogger;
import org.json.JSONObject;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

public class SmartRoad_aws {
    private String roadSegment;
    private String loggerId;
    private AWSIoTManager awsManager;
    
    public SmartRoad_aws(String roadSegment) {
        this.roadSegment = roadSegment;
        this.loggerId = "SmartRoad_aws-" + roadSegment;
        MySimpleLogger.info(loggerId, "Constructor llamado para segmento: " + roadSegment);
    }
    
    public void initAWS(String endpoint, String thingName, String certPath, String keyPath, String caPath) {
        try {
            this.awsManager = new AWSIoTManager(endpoint, thingName, certPath, keyPath, caPath);
            this.awsManager.connect();
            
            // Suscribirse para recibir comandos con callback
            String deltaTopic = "$aws/things/" + thingName + "/shadow/update/delta";
            
            try {
                // Intentar usar AWS SDK con callback
                this.awsManager.subscribeWithCallback(deltaTopic, 1, new com.amazonaws.services.iot.client.core.AwsIotTopicCallback() {
                    @Override
                    public void onMessage(com.amazonaws.services.iot.client.AWSIotMessage message) {
                        String payload = message.getStringPayload();
                        MySimpleLogger.info(loggerId, "[AWS-SmartRoad] Mensaje recibido: " + payload);
                        
                        try {
                            JSONObject json = new JSONObject(payload);
                            if (json.has("state")) {
                                JSONObject state = json.getJSONObject("state");
                                // Procesar comandos aquí
                                MySimpleLogger.info(loggerId, "Comando recibido: " + state.toString());
                            }
                        } catch (Exception e) {
                            MySimpleLogger.error(loggerId, "Error procesando mensaje: " + e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                // Fallback a Paho
                this.awsManager.setCallback(new MqttCallback() {
                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        String payload = new String(message.getPayload());
                        MySimpleLogger.info(loggerId, "[AWS-SmartRoad] Mensaje recibido: " + payload);
                        
                        try {
                            JSONObject json = new JSONObject(payload);
                            if (json.has("state")) {
                                JSONObject state = json.getJSONObject("state");
                                // Procesar comandos aquí
                                MySimpleLogger.info(loggerId, "Comando recibido: " + state.toString());
                            }
                        } catch (Exception ex) {
                            MySimpleLogger.error(loggerId, "Error procesando mensaje: " + ex.getMessage());
                        }
                    }
                    
                    @Override
                    public void connectionLost(Throwable cause) {
                        MySimpleLogger.error(loggerId, "Conexión AWS perdida: " + (cause != null ? cause.getMessage() : "Desconocido"));
                    }
                    
                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        // No necesario
                    }
                });
                this.awsManager.subscribe(deltaTopic, 1);
            }
            
            MySimpleLogger.info(loggerId, "✓ AWS IoT inicializado para SmartRoad: " + thingName);
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error inicializando AWS en SmartRoad: " + e.getMessage());
            MySimpleLogger.warn(loggerId, "Continuando sin AWS IoT. Funcionalidad local seguirá activa.");
            this.awsManager = null; // Marcar como no disponible
        }
    }
    
    public void disconnectAWS() {
        if (this.awsManager != null) {
            try {
                this.awsManager.disconnect();
                MySimpleLogger.info(loggerId, "AWS IoT desconectado para SmartRoad");
            } catch (Exception e) {
                MySimpleLogger.error(loggerId, "Error desconectando AWS: " + e.getMessage());
            }
        }
    }
}