package smartroad.impl;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;
import dispositivo.utils.MySimpleLogger;


import dispositivo.componentes.AWSIoTManager;


import smartroad.impl.SmartRoad_IncidentNotifier;
import smartroad.impl.SmartRoad_RoadIncidentsSubscriber;

public class SmartRoad_aws {
    
    protected SmartRoad_IncidentNotifier notifier = null;
    protected SmartRoad_RoadIncidentsSubscriber subscriber = null;
    protected String id = null;
    
    // >>> CAMPOS AWS
    private AWSIoTManager awsManager = null;
    private String awsEndpoint;
    private String awsThingName;

    public SmartRoad_aws(String id) {
        this.setId(id);
        this.subscriber = new SmartRoad_RoadIncidentsSubscriber(this);
        this.subscriber.connect();
        this.subscriber.subscribe("es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + id + "/alerts");
        
        // Inicializar y conectar el notifier para retransmitir alertas al canal info
        this.notifier = new SmartRoad_IncidentNotifier(this);
        this.notifier.connect();
        
        MySimpleLogger.info("SmartRoad-" + id, "SmartRoad inicializado para segmento: " + id);
    }

    /**
     * Inicializar AWS IoT
     */
    public void initAWS(String endpoint, String thingName, String certPath, String keyPath, String caPath) {
        this.awsEndpoint = endpoint;
        this.awsThingName = thingName;
        this.awsManager = new AWSIoTManager(endpoint, thingName, certPath, keyPath, caPath);
        
        try {
            this.awsManager.connect();
            
            // Suscribir a alerts AWS 
            String awsAlertsTopic = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + this.id + "/alerts";
            this.awsManager.subscribe(awsAlertsTopic, message -> {
                try {
                    String awsAlert = new String(message.getPayload());
                    MySimpleLogger.info("SmartRoad-" + id, "[AWS-ALERTS] Recibido: " + awsAlert);
                    
                    // Retransmitir AWS alert → info AWS
                    String awsInfoTopic = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + this.id + "/info";
                    awsManager.publish(awsInfoTopic, awsAlert);
                    
                    // Retransmitir a LOCAL
                    notify(awsAlert);
                    
                } catch (Exception e) {
                    MySimpleLogger.error("SmartRoad-" + id, "Error procesar AWS alert: " + e.getMessage());
                }
            });
            
            MySimpleLogger.info("SmartRoad-" + id, "✓ AWS IoT inicializado para SmartRoad: " + thingName);
            
        } catch (Exception e) {
            MySimpleLogger.error("SmartRoad-" + id, "Error AWS init: " + e.getMessage());
        }
    }

    /**
     * Publicar estado en AWS Shadow
     */
    private void publishAwsState() {
        if (awsManager == null) return;
        
        try {
            JSONObject state = new JSONObject();
            state.put("road_segment", this.id);
            state.put("status", "active");
            state.put("local_subscriber", subscriber != null && subscriber.isConnected());
            state.put("local_notifier", notifier != null && notifier.isConnected());
            state.put("aws_connected", awsManager.isConnected());
            state.put("timestamp", System.currentTimeMillis());
            
            awsManager.updateDeviceShadow(state.toString());
            MySimpleLogger.info("SmartRoad-" + id, "Shadow actualizado: segmento " + this.id);
            
        } catch (Exception e) {
            MySimpleLogger.error("SmartRoad-" + id, "Error publish AWS state: " + e.getMessage());
        }
    }

    /**
     * Retransmite un mensaje recibido en /alerts al canal /info
     * @param message El mensaje JSON a retransmitir
     * Publica en AWS
     */
    public void notify(String message) {
        
        if (this.notifier != null) {
            this.notifier.notify(message);
            MySimpleLogger.info("SmartRoad-" + id, "Alert retransmitido LOCAL → /info");
        }
        
        // AWS 
        if (this.awsManager != null) {
            try {
                String awsInfoTopic = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + this.id + "/info";
                awsManager.publish(awsInfoTopic, message);
                MySimpleLogger.info("SmartRoad-" + id, "Alert retransmitido AWS → /info");
                
                // Actualizar shadow
                publishAwsState();
                
            } catch (Exception e) {
                MySimpleLogger.error("SmartRoad-" + id, "Error AWS notify: " + e.getMessage());
            }
        }
    }

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }

    /**
     *  Desconectar AWS
     */
    public void disconnectAWS() {
        if (this.awsManager != null) {
            try {
                this.awsManager.disconnect();
                MySimpleLogger.info("SmartRoad-" + id, "AWS IoT desconectado: " + this.id);
            } catch (Exception e) {
                MySimpleLogger.error("SmartRoad-" + id, "Error AWS disconnect: " + e.getMessage());
            }
        }
    }

    /**
     *  Estado de conexiones
     */
    public String getConnectionStatus() {
        StringBuilder status = new StringBuilder();
        status.append("SmartRoad ").append(id).append(":\n");
        status.append("- LOCAL Subscriber: ").append(subscriber != null && subscriber.isConnected() ? "OK" : "OFFLINE").append("\n");
        status.append("- LOCAL Notifier: ").append(notifier != null && notifier.isConnected() ? "OK" : "OFFLINE").append("\n");
        status.append("- AWS IoT: ").append(awsManager != null && awsManager.isConnected() ? "OK" : "OFFLINE").append("\n");
        return status.toString();
    }
}
