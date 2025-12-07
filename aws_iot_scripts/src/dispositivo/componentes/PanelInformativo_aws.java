package dispositivo.componentes;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import dispositivo.interfaces.FuncionStatus;
import dispositivo.interfaces.IDispositivo;
import dispositivo.interfaces.IFuncion;
import dispositivo.utils.MySimpleLogger;

public class PanelInformativo_aws implements MqttCallback {
    
    private MqttClient mqttClientSubscriber;
    private MqttClient mqttClientPublisher;
    private String roadSegment;
    private String loggerId;
    private String ttmiID;
    private String topicBase;
    private String topicInfo;
    private String topicTraffic;
    private String topicAlerts;
    private int ubicacionInicial;

    private IDispositivo semaforo;

    Map<String, Integer> vehiculosEspecialesEnSegmentoCerca = new HashMap<>();
    Map<String, Integer> vehiculosEspecialesEnSegmentoLejos = new HashMap<>();

    private int contadorAccidentes = 0;

    // AWS IoT components (siguiendo patrón de smartcar)
    private PanelInformativo_AWSShadowPublisher awsShadowPublisher = null;
    private PanelInformativo_AWSShadowSubscriber awsShadowSubscriber = null;
    private String awsEndpoint;
    private String awsThingName;

    public PanelInformativo_aws(String mqttBroker, String ttmiID, String roadSegment, int ubicacionInicial) {
        this.roadSegment = roadSegment;
        this.loggerId = "PanelInformativo" + "-" + ttmiID;
        this.ttmiID = ttmiID;
        this.ubicacionInicial = ubicacionInicial;
        this.topicBase = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/";
        this.topicInfo = topicBase + roadSegment + "/info";
        this.topicTraffic = topicBase + roadSegment + "/traffic";
        this.topicAlerts = topicBase + roadSegment + "/alerts";

        // Crear dispositivo y funciones
        try {
            // Crear un dispositivo usando el método build estático
            // Necesitamos IP y broker MQTT, pero para este caso usamos valores por defecto
            String localIP = "127.0.0.1";
            String localBroker = mqttBroker;
            this.semaforo = Dispositivo.build(ttmiID, localIP, localBroker);
            
            IFuncion f1 = Funcion.build("f1", FuncionStatus.OFF);
            IFuncion f2 = Funcion.build("f2", FuncionStatus.OFF);
            IFuncion f3 = Funcion.build("f3", FuncionStatus.OFF);
            
            semaforo.addFuncion(f1);
            semaforo.addFuncion(f2);
            semaforo.addFuncion(f3);
            
            semaforo.iniciar();
            
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error creando semáforo: " + e.getMessage());
            // Crear dispositivo de respaldo
            try {
                String localIP = "127.0.0.1";
                String localBroker = mqttBroker;
                this.semaforo = Dispositivo.build("fallback", localIP, localBroker);
            } catch (Exception e2) {
                MySimpleLogger.error(loggerId, "Error creando dispositivo de respaldo: " + e2.getMessage());
            }
        }

        try {
            this.mqttClientSubscriber = new MqttClient(mqttBroker, "PanelInformativo_Sub", new MemoryPersistence());
            this.mqttClientPublisher = new MqttClient(mqttBroker, "PanelInformativo_Pub", new MemoryPersistence());
            
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setKeepAliveInterval(60);
            
            this.mqttClientSubscriber.setCallback(this);
            
            this.mqttClientSubscriber.connect(connOpts);
            this.mqttClientPublisher.connect(connOpts);
            MySimpleLogger.info(loggerId, "Conectado al broker MQTT LOCAL: " + mqttBroker);
            
        } catch (MqttException e) {
            MySimpleLogger.error(loggerId, "Error al conectar con MQTT LOCAL: " + e.getMessage());
            // No lanzamos excepción para permitir que el programa continúe
        }
    }

    /**
     * Inicializar AWS IoT (siguiendo patrón de smartcar)
     * Nota: Si la conexión falla, el programa continuará sin AWS pero con funcionalidad MQTT local
     */
    public void initAWS(String endpoint, String thingName, String certPath, String keyPath, String caPath) {
        this.awsEndpoint = endpoint;
        this.awsThingName = thingName;
        
        try {
            // Device Shadow: Publicar estado
            if (this.awsShadowPublisher == null) {
                this.awsShadowPublisher = new PanelInformativo_AWSShadowPublisher(this, thingName, endpoint, certPath, keyPath);
                this.awsShadowPublisher.connect();
            }
            
            // Device Shadow: Recibir comandos
            if (this.awsShadowSubscriber == null) {
                String deltaTopic = "$aws/things/" + thingName + "/shadow/update/delta";
                this.awsShadowSubscriber = new PanelInformativo_AWSShadowSubscriber(this, thingName, endpoint, certPath, keyPath, deltaTopic);
                this.awsShadowSubscriber.connect();
            }
            
            MySimpleLogger.info(loggerId, "✓ AWS IoT inicializado para panel: " + thingName);
            MySimpleLogger.info(loggerId, "  - Device Shadow: enabled");
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error AWS init: " + e.getMessage());
            MySimpleLogger.warn(loggerId, "Continuando sin AWS IoT. Funcionalidad MQTT local seguirá activa.");
            this.awsShadowPublisher = null;
            this.awsShadowSubscriber = null;
        }
    }

    // Métodos públicos para que las clases AWS puedan acceder
    public void aplicarComandoF1(String accion) {
        try {
            switch (accion.toLowerCase()) {
                case "on" -> semaforo.getFuncion("f1").encender();
                case "off" -> semaforo.getFuncion("f1").apagar();
                case "blink" -> semaforo.getFuncion("f1").parpadear();
            }
            MySimpleLogger.info(loggerId, "AWS → Forzado f1: " + accion);
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error f1 AWS: " + e.getMessage());
        }
    }

    public void aplicarComandoF2(String accion) {
        try {
            switch (accion.toLowerCase()) {
                case "on" -> semaforo.getFuncion("f2").encender();
                case "off" -> semaforo.getFuncion("f2").apagar();
            }
            MySimpleLogger.info(loggerId, "AWS → Forzado f2: " + accion);
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error f2 AWS: " + e.getMessage());
        }
    }

    public void aplicarComandoF3(String accion) {
        try {
            switch (accion.toLowerCase()) {
                case "on" -> semaforo.getFuncion("f3").encender();
                case "off" -> semaforo.getFuncion("f3").apagar();
                case "blink" -> semaforo.getFuncion("f3").parpadear();
            }
            MySimpleLogger.info(loggerId, "AWS → Forzado f3: " + accion);
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error f3 AWS: " + e.getMessage());
        }
    }

    // Método para construir el estado reported (usado por AWSShadowPublisher)
    public JSONObject buildReportedState() {
        JSONObject state = new JSONObject();
        try {
            state.put("road_segment", roadSegment);
            state.put("ttmi_id", ttmiID);
            state.put("accidents_count", contadorAccidentes);
            state.put("special_vehicles_near", vehiculosEspecialesEnSegmentoCerca.size());
            state.put("special_vehicles_far", vehiculosEspecialesEnSegmentoLejos.size());
            
            // Solo si tenemos funciones disponibles
            if (semaforo.getFuncion("f1") != null) {
                state.put("f1_status", semaforo.getFuncion("f1").getStatus().toString());
            }
            if (semaforo.getFuncion("f2") != null) {
                state.put("f2_status", semaforo.getFuncion("f2").getStatus().toString());
            }
            if (semaforo.getFuncion("f3") != null) {
                state.put("f3_status", semaforo.getFuncion("f3").getStatus().toString());
            }
            
            state.put("timestamp", System.currentTimeMillis());
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error building reported state: " + e.getMessage());
        }
        return state;
    }
    
    // Método para notificar que el estado cambió (llamado por subscriber después de aplicar comandos)
    public void notifyStateChanged() {
        publishAwsState();
    }
    
    private void publishAwsState() {
        if (awsShadowPublisher != null && awsShadowPublisher.isConnected()) {
            awsShadowPublisher.publishState();
        }
    }
    
    // Getters para las clases AWS
    public String getTtmiID() { return ttmiID; }
    public String getLoggerId() { return loggerId; }

    public void iniciar() {
        MySimpleLogger.info(loggerId, "=== INICIANDO PanelInformativo ===");
        MySimpleLogger.info(loggerId, "RoadSegment: " + roadSegment);
        
        if (mqttClientSubscriber != null && mqttClientSubscriber.isConnected()) {
            try {
                mqttClientSubscriber.subscribe(topicInfo, 0);
                MySimpleLogger.info(loggerId, "Suscrito al topic: " + topicInfo);
                
                mqttClientSubscriber.subscribe(topicTraffic, 0);
                MySimpleLogger.info(loggerId, "Suscrito al topic: " + topicTraffic);
                
                mqttClientSubscriber.subscribe(topicAlerts, 0);
                MySimpleLogger.info(loggerId, "Suscrito al topic: " + topicAlerts);
            } catch (MqttException e) {
                MySimpleLogger.error(loggerId, "Error al suscribirse a topics: " + e.getMessage());
            }
        } else {
            MySimpleLogger.warn(loggerId, "Cliente MQTT no conectado, no se pueden suscribir a topics");
        }
    }
    
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        MySimpleLogger.info(loggerId, "Mensaje recibido en topic " + topic + ": " + payload);
        
        try {
            JSONObject statusMessage = new JSONObject(payload);
            JSONObject msg = statusMessage.getJSONObject("msg");

            if (topic.equals(topicTraffic)) {
                String status = msg.getString("status");
                MySimpleLogger.info(loggerId, "Status obtenido: " + status);

                switch (status) {
                    case "Free_Flow", "Mostly_Free_Flow" -> {
                        if (semaforo.getFuncion("f1") != null) semaforo.getFuncion("f1").apagar();
                        MySimpleLogger.info(loggerId, "Apagando f1");
                    }
                    case "Limited_Manouvers" -> {
                        if (semaforo.getFuncion("f1") != null) semaforo.getFuncion("f1").parpadear();  
                        MySimpleLogger.info(loggerId, "Parpadeando f1");
                    }
                    case "No_Manouvers", "Collapsed" -> {
                        if (semaforo.getFuncion("f1") != null) semaforo.getFuncion("f1").encender();
                        MySimpleLogger.info(loggerId, "Encendiendo f1");
                    }
                    default -> MySimpleLogger.warn(loggerId, "Estado desconocido: " + status);
                }
                publishAwsState();
                
            } else if (topic.equals(topicAlerts)) {
                String type = statusMessage.getString("type");

                if (type.equals("ACCIDENT")) {
                    String accidenteId = msg.getString("id");
                    String event = msg.getString("event");

                    if (event.equals("OPEN")) {
                        contadorAccidentes++;
                    } else if (event.equals("CLOSE")) {
                        contadorAccidentes = Math.max(0, contadorAccidentes - 1);
                    }

                    MySimpleLogger.info(loggerId, "Accidente " + accidenteId + " (" + event + "): número de accidentes: " + contadorAccidentes);

                    if (contadorAccidentes > 0 && semaforo.getFuncion("f2") != null) {
                        semaforo.getFuncion("f2").encender();
                        MySimpleLogger.info(loggerId, "Encendiendo f2");
                    } else if (semaforo.getFuncion("f2") != null) {
                        semaforo.getFuncion("f2").apagar();
                        MySimpleLogger.info(loggerId, "Apagando f2");
                    }
                }
                publishAwsState();
                
            } else if (topic.equals(topicInfo)) {
                String action = msg.getString("action");
                String vehicleRole = msg.getString("vehicle-role");
                String vehicleId = msg.getString("vehicle-id");
                int position = msg.getInt("position");

                boolean esVehiculoEspecial = vehicleRole.equals("Ambulance") || vehicleRole.equals("Police");
                int diferenciaPosicion = Math.abs(position - ubicacionInicial);

                if (esVehiculoEspecial) {
                    if ("VEHICLE_IN".equals(action)) {
                        if (diferenciaPosicion < 200) {
                            vehiculosEspecialesEnSegmentoCerca.put(vehicleId, position);
                            vehiculosEspecialesEnSegmentoLejos.remove(vehicleId);
                        } else {
                            vehiculosEspecialesEnSegmentoLejos.put(vehicleId, position);
                            vehiculosEspecialesEnSegmentoCerca.remove(vehicleId);
                        }
                        
                    } else if ("VEHICLE_OUT".equals(action)) {
                        vehiculosEspecialesEnSegmentoCerca.remove(vehicleId);
                        vehiculosEspecialesEnSegmentoLejos.remove(vehicleId);
                    }

                    int vehiculosCerca = vehiculosEspecialesEnSegmentoCerca.size();
                    int vehiculosLejos = vehiculosEspecialesEnSegmentoLejos.size();

                    if (vehiculosCerca == 0 && vehiculosLejos == 0 && semaforo.getFuncion("f3") != null) {
                        semaforo.getFuncion("f3").apagar();
                        MySimpleLogger.info(loggerId, "Apagando f3");
                    } else if (vehiculosCerca > 0 && semaforo.getFuncion("f3") != null) {
                        semaforo.getFuncion("f3").parpadear();
                        MySimpleLogger.info(loggerId, "Parpadeando f3");
                    } else if (vehiculosLejos > 0 && semaforo.getFuncion("f3") != null) {
                        semaforo.getFuncion("f3").encender();
                        MySimpleLogger.info(loggerId, "Encendiendo f3");
                    }
                }
                publishAwsState();
                
            } else {
                MySimpleLogger.warn(loggerId, "Topic desconocido: " + topic);
            }
            
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error al procesar mensaje: " + e.getMessage());
        }
    }

    public void disconnectAWS() {
        try {
            if (this.awsShadowPublisher != null) {
                this.awsShadowPublisher.disconnect();
            }
            if (this.awsShadowSubscriber != null) {
                this.awsShadowSubscriber.disconnect();
            }
            MySimpleLogger.info(loggerId, "AWS IoT desconectado");
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error AWS disconnect: " + e.getMessage());
        }
    }

    public void cerrar() {
        try {
            if (mqttClientSubscriber != null && mqttClientSubscriber.isConnected()) {
                mqttClientSubscriber.disconnect();
                mqttClientSubscriber.close();
                MySimpleLogger.info(loggerId, "Conexión MQTT LOCAL Subscriber cerrada");
            }
            if (mqttClientPublisher != null && mqttClientPublisher.isConnected()) {
                mqttClientPublisher.disconnect();
                mqttClientPublisher.close();
                MySimpleLogger.info(loggerId, "Conexión MQTT LOCAL Publisher cerrada");
            }
            disconnectAWS();
        } catch (MqttException e) {
            MySimpleLogger.error(loggerId, "Error al cerrar conexiones: " + e.getMessage());
        }
    }
    
    @Override
    public void connectionLost(Throwable cause) {
        MySimpleLogger.error(loggerId, "Conexión MQTT LOCAL perdida: " + (cause != null ? cause.getMessage() : "Desconocido"));
    }
    
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // No necesitamos hacer nada aquí
    }
}