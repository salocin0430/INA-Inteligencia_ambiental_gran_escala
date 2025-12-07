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

// AWS IMPORT 
import dispositivo_componentes.AWSIoTManager;

public class PanelInformativo_aws implements MqttCallback {
    
    private MqttClient mqttClientSubscriber;  // Para suscribirse (recibir)
    private MqttClient mqttClientPublisher;   // Para publicar
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

    private int contadorAccidentes = 0;  // Contador inicializado a 0

    // >>> CAMPOS AWS
    private AWSIoTManager awsManager = null;
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

        this.semaforo = Dispositivo.build(ttmiID, ttmiID + ".iot.upv.es", 8182, mqttBroker);
        IFuncion f1 = Funcion.build("f1", FuncionStatus.OFF);
        semaforo.addFuncion(f1);
        IFuncion f2 = Funcion.build("f2", FuncionStatus.OFF);
        semaforo.addFuncion(f2);
        IFuncion f3 = Funcion.build("f3", FuncionStatus.OFF);
        semaforo.addFuncion(f3);
        // Arrancamos el semáforo
        semaforo.iniciar();

        try {
            // Crear cliente MQTT para suscripciones (recibir)
            this.mqttClientSubscriber = new MqttClient(mqttBroker, "PanelInformativo_Sub", new MemoryPersistence());
            
            // Crear cliente MQTT para publicaciones (enviar)
            this.mqttClientPublisher = new MqttClient(mqttBroker, "PanelInformativo_Pub", new MemoryPersistence());
            
            // Configurar opciones de conexión
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setKeepAliveInterval(60);
            
            // Configurar callback solo en el subscriber
            this.mqttClientSubscriber.setCallback(this);
            
            // Conectar ambos clientes al broker
            this.mqttClientSubscriber.connect(connOpts);
            this.mqttClientPublisher.connect(connOpts);
            MySimpleLogger.info(loggerId, "Conectado al broker MQTT LOCAL: " + mqttBroker);
            
        } catch (MqttException e) {
            MySimpleLogger.error(loggerId, "Error al conectar con MQTT LOCAL: " + e.getMessage());
            throw new RuntimeException("No se pudo conectar al broker MQTT LOCAL", e);
        }
    }

    /**
     *  Inicializar AWS IoT
     */
    public void initAWS(String endpoint, String thingName, String certPath, String keyPath, String caPath) {
        this.awsEndpoint = endpoint;
        this.awsThingName = thingName;
        this.awsManager = new AWSIoTManager(endpoint, thingName, certPath, keyPath, caPath);
        
        try {
            this.awsManager.connect();
            
            // Suscribir a cambios de desired (control remoto de LEDs desde AWS)
            this.awsManager.subscribeToShadowUpdates(message -> {
                String deltaJson = new String(message.getPayload());
                MySimpleLogger.info(loggerId, "[AWS-DELTA-PANEL] " + deltaJson);
                
                try {
                    JSONObject root = new JSONObject(deltaJson);
                    JSONObject state = root.getJSONObject("state");
                    
                    // Si AWS manda desired.f1, f2, f3 → forzar LEDs
                    if (state.has("f1")) {
                        String accionF1 = state.getString("f1");
                        aplicarComandoF1(accionF1);
                    }
                    if (state.has("f2")) {
                        String accionF2 = state.getString("f2");
                        aplicarComandoF2(accionF2);
                    }
                    if (state.has("f3")) {
                        String accionF3 = state.getString("f3");
                        aplicarComandoF3(accionF3);
                    }
                    
                    publishAwsState(); // Confirmar que se aplicó
                } catch (Exception e) {
                    MySimpleLogger.error(loggerId, "Error procesando AWS delta: " + e.getMessage());
                }
            });
            
            MySimpleLogger.info(loggerId, "✓ AWS IoT inicializado para panel: " + thingName);
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error AWS init: " + e.getMessage());
        }
    }

    /**
     *  Métodos auxiliares para aplicar comandos F1/F2/F3 desde AWS
     */
    private void aplicarComandoF1(String accion) {
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

    private void aplicarComandoF2(String accion) {
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

    private void aplicarComandoF3(String accion) {
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

    /**
     * Publicar estado en AWS Shadow
     */
    private void publishAwsState() {
        if (awsManager == null) return;
        try {
            JSONObject state = new JSONObject();
            state.put("road_segment", roadSegment);
            state.put("ttmi_id", ttmiID);
            state.put("accidents_count", contadorAccidentes);
            state.put("special_vehicles_near", vehiculosEspecialesEnSegmentoCerca.size());
            state.put("special_vehicles_far", vehiculosEspecialesEnSegmentoLejos.size());
            state.put("f1_status", semaforo.getFuncion("f1").getStatus().toString());
            state.put("f2_status", semaforo.getFuncion("f2").getStatus().toString());
            state.put("f3_status", semaforo.getFuncion("f3").getStatus().toString());
            state.put("timestamp", System.currentTimeMillis());
            
            awsManager.updateDeviceShadow(state.toString());
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error publish AWS: " + e.getMessage());
        }
    }

    /**
     * Inicia el PanelInformativo
     */
    public void iniciar() throws MqttException {
        MySimpleLogger.info(loggerId, "=== INICIANDO PanelInformativo ===");
        MySimpleLogger.info(loggerId, "RoadSegment: " + roadSegment);
        
        // Suscribirse al topic de información
        MySimpleLogger.info(loggerId, "Suscribiendo al topic: " + topicInfo);
        mqttClientSubscriber.subscribe(topicInfo, 0);
        MySimpleLogger.info(loggerId, "Suscrito al topic: " + topicInfo);
        
        MySimpleLogger.info(loggerId, "Suscribiendo al topic: " + topicTraffic);
        mqttClientSubscriber.subscribe(topicTraffic, 0);
        MySimpleLogger.info(loggerId, "Suscrito al topic: " + topicTraffic);
        
        MySimpleLogger.info(loggerId, "Suscribiendo al topic: " + topicAlerts);
        mqttClientSubscriber.subscribe(topicAlerts, 0);        
        MySimpleLogger.info(loggerId, "Suscrito al topic: " + topicAlerts);
    }
    
    /**
     * Callback cuando llega un mensaje MQTT
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        MySimpleLogger.info(loggerId, "Mensaje recibido en topic " + topic + ": " + payload);
        MySimpleLogger.info(loggerId, "Cliente MQTT Subscriber conectado: " + (mqttClientSubscriber != null && mqttClientSubscriber.isConnected()));
        MySimpleLogger.info(loggerId, "Cliente MQTT Publisher conectado: " + (mqttClientPublisher != null && mqttClientPublisher.isConnected()));
        
        try {
            // Parsear el mensaje JSON
            JSONObject statusMessage = new JSONObject(payload);
            JSONObject msg = statusMessage.getJSONObject("msg");

            if (topic.equals(topicTraffic)) {
                // Obtener el valor del campo "status"
                String status = msg.getString("status");
                MySimpleLogger.info(loggerId, "Status obtenido: " + status);

                switch (status) {
                    case "Free_Flow", "Mostly_Free_Flow" -> {
                        semaforo.getFuncion("f1").apagar();
                        MySimpleLogger.info(loggerId, "Apagando f1");
                    }
                    case "Limited_Manouvers" -> {
                        semaforo.getFuncion("f1").parpadear();  
                        MySimpleLogger.info(loggerId, "Parpadeando f1");
                    }
                    case "No_Manouvers", "Collapsed" -> {
                        semaforo.getFuncion("f1").encender();
                        MySimpleLogger.info(loggerId, "Encendiendo f1");
                    }
                    default -> {
                        MySimpleLogger.warn(loggerId, "Estado desconocido: " + status);
                    }
                }
                publishAwsState(); //  Publicar en AWS después de cambio
                
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

                    if (contadorAccidentes > 0) {
                        semaforo.getFuncion("f2").encender();
                        MySimpleLogger.info(loggerId, "Encendiendo f2");
                    } else {
                        semaforo.getFuncion("f2").apagar();
                        MySimpleLogger.info(loggerId, "Apagando f2");
                    }
                }
                publishAwsState(); // Publicar en AWS después de cambio
                
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

                    if (vehiculosCerca == 0 && vehiculosLejos == 0) {
                        semaforo.getFuncion("f3").apagar();
                        MySimpleLogger.info(loggerId, "Apagando f3");
                    } else if (vehiculosCerca > 0) {
                        semaforo.getFuncion("f3").parpadear();
                        MySimpleLogger.info(loggerId, "Parpadeando f3");
                    } else if (vehiculosLejos > 0) {
                        semaforo.getFuncion("f3").encender();
                        MySimpleLogger.info(loggerId, "Encendiendo f3");
                    }
                }
                publishAwsState(); // Publicar en AWS después de cambio
                
            } else {
                MySimpleLogger.warn(loggerId, "Topic desconocido: " + topic);
            }
            
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error al procesar mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Desconectar AWS
     */
    public void disconnectAWS() {
        if (this.awsManager != null) {
            try {
                this.awsManager.disconnect();
                MySimpleLogger.info(loggerId, "AWS IoT desconectado");
            } catch (Exception e) {
                MySimpleLogger.error(loggerId, "Error AWS disconnect: " + e.getMessage());
            }
        }
    }

    /**
     * Cierra la conexión MQTT LOCAL
     */
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
            if (awsManager != null) {
                awsManager.disconnect();
                MySimpleLogger.info(loggerId, "AWS IoT desconectado");
            }
        } catch (MqttException e) {
            MySimpleLogger.error(loggerId, "Error al cerrar conexiones: " + e.getMessage());
        }
    }
    
    // Métodos de callback requeridos por MqttCallback
    @Override
    public void connectionLost(Throwable cause) {
        MySimpleLogger.error(loggerId, "Conexión MQTT LOCAL perdida: " + (cause != null ? cause.getMessage() : "Desconocido"));
        if (cause != null) {
            cause.printStackTrace();
        }
        
        // Intentar reconectar automáticamente con retry
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                MySimpleLogger.info(loggerId, "Intentando reconectar... (intento " + (retryCount + 1) + "/" + maxRetries + ")");
                
                if (mqttClientSubscriber != null && !mqttClientSubscriber.isConnected()) {
                    // Configurar opciones de conexión
                    MqttConnectOptions connOpts = new MqttConnectOptions();
                    connOpts.setCleanSession(true);
                    connOpts.setKeepAliveInterval(60);
                    
                    mqttClientSubscriber.connect(connOpts);
                    
                    // Re-suscribirse a topics
                    mqttClientSubscriber.subscribe(topicInfo, 0);
                    MySimpleLogger.info(loggerId, "Reconectado y re-suscrito al topic: " + topicInfo);
                    mqttClientSubscriber.subscribe(topicTraffic, 0);
                    MySimpleLogger.info(loggerId, "Reconectado y re-suscrito al topic: " + topicTraffic);
                    mqttClientSubscriber.subscribe(topicAlerts, 0);
                    MySimpleLogger.info(loggerId, "Reconectado y re-suscrito al topic: " + topicAlerts);
                    return; // Éxito, salir del bucle
                }
            } catch (MqttException e) {
                retryCount++;
                MySimpleLogger.error(loggerId, "Error al reconectar (intento " + retryCount + "): " + e.getMessage());
                
                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(2000); // Esperar 2 segundos antes del siguiente intento
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        MySimpleLogger.error(loggerId, "No se pudo reconectar después de " + maxRetries + " intentos");
    }
    
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // No necesitamos hacer nada aquí
    }
}
