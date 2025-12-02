package dispositivo.componentes;

import org.json.JSONObject;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import dispositivo.utils.MySimpleLogger;


import aws.AWSIoTManager;  // Clase auxiliar para manejar AWS IoT

public class SignalSpeed implements MqttCallback {
    
    private String roadSegment;
    private String id;
    private int velocidadMaxima;
    private int posicionInicio;
    private int posicionFin;
    private MqttClient mqttClientPublisher;   // Para publicar LOCAL
    private String topicPublicacion;
    private String loggerId;

    // >>> AWS CAMPOS NUEVOS
    private AWSIoTManager awsManager = null;
    private String awsEndpoint;
    private String awsThingName;
    private boolean señalActiva = false;  // Estado actual de la señal

    public SignalSpeed(String roadSegment, String id, int velocidadMaxima, int posicionInicio, int posicionFin,
                      String mqttBroker) {
        this.roadSegment = roadSegment;
        this.id = id;
        this.velocidadMaxima = velocidadMaxima;
        this.posicionInicio = posicionInicio;
        this.posicionFin = posicionFin;
        this.topicPublicacion = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + roadSegment + "/signals";
        this.loggerId = "SignalSpeed-" + id;

        try {
            // Crear cliente MQTT para publicaciones LOCAL (sin cambios)
            this.mqttClientPublisher = new MqttClient(mqttBroker, "SignalSpeed_Pub_" + id, new MemoryPersistence());
            
            // Configurar opciones de conexión LOCAL
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setKeepAliveInterval(60);
            
            this.mqttClientPublisher.connect(connOpts);
            MySimpleLogger.info(loggerId, "Conectado al broker MQTT LOCAL: " + mqttBroker);
            
        } catch (MqttException e) {
            MySimpleLogger.error(loggerId, "Error al conectar con MQTT LOCAL: " + e.getMessage());
            throw new RuntimeException("No se pudo conectar al broker MQTT LOCAL", e);
        }
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
            
            // Suscribir a comandos AWS (desired.speedlimit)
            this.awsManager.subscribeToShadowUpdates(message -> {
                try {
                    String deltaJson = new String(message.getPayload());
                    JSONObject root = new JSONObject(deltaJson);
                    JSONObject state = root.getJSONObject("state");
                    
                    if (state.has("activate")) {
                        boolean activate = state.getBoolean("activate");
                        if (activate) {
                            this.señalActiva = true;
                            publicarEstado();  // Publicar señal activa
                        } else {
                            this.señalActiva = false;
                        }
                        publishAwsState();
                        MySimpleLogger.info(loggerId, "AWS → Señal " + (activate ? "ACTIVADA" : "DESACTIVADA"));
                    }
                } catch (Exception e) {
                    MySimpleLogger.error(loggerId, "Error AWS delta SignalSpeed: " + e.getMessage());
                }
            });
            
            MySimpleLogger.info(loggerId, "✓ AWS IoT inicializado para SignalSpeed: " + thingName);
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error AWS init SignalSpeed: " + e.getMessage());
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
            state.put("signal_id", id);
            state.put("speed_limit", velocidadMaxima);
            state.put("position_start", posicionInicio);
            state.put("position_end", posicionFin);
            state.put("active", señalActiva);
            state.put("timestamp", System.currentTimeMillis());
            
            awsManager.updateDeviceShadow(state.toString());
            MySimpleLogger.info(loggerId, "Shadow SignalSpeed actualizado: " + velocidadMaxima + "km/h " + (señalActiva ? "ACTIVA" : "INACTIVA"));
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error publish AWS SignalSpeed: " + e.getMessage());
        }
    }

    // Forma y envía el mensaje JSON con el estado de la señal (MODIFICADO con AWS)
    public void publicarEstado() {
        try {
            JSONObject root = new JSONObject();
            JSONObject msg = new JSONObject();

            msg.put("signal-type", "SPEED_LIMIT");
            msg.put("rt", "traffic-signal");
            msg.put("id", this.id);
            msg.put("road-segment", this.roadSegment);
            msg.put("starting-position", this.posicionInicio);
            msg.put("ending-position", this.posicionFin);
            msg.put("value", this.velocidadMaxima);

            root.put("msg", msg);
            root.put("id", "MSG_" + System.currentTimeMillis());
            root.put("type", "TRAFFIC_SIGNAL");
            root.put("timestamp", System.currentTimeMillis());

            String payload = root.toString();

            
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            message.setRetained(false);
            mqttClientPublisher.publish(topicPublicacion, message);
            MySimpleLogger.info(loggerId, "Mensaje LOCAL publicado en: " + topicPublicacion);

            // >>> AWS 
            if (this.awsManager != null) {
                String awsTopic = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + roadSegment + "/signals";
                awsManager.publish(awsTopic, payload);
                MySimpleLogger.info(loggerId, "Mensaje AWS publicado en: " + awsTopic);
                
                // Actualizar shadow
                this.señalActiva = true;
                publishAwsState();
            }

        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error publicando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Activar señal remotamente
     */
    public void activarSeñal() {
        this.señalActiva = true;
        publicarEstado();
        publishAwsState();
        MySimpleLogger.info(loggerId, "Señal ACTIVADA: " + velocidadMaxima + "km/h");
    }

    /**
     *  Desactivar señal remotamente
     */
    public void desactivarSeñal() {
        this.señalActiva = false;
        publishAwsState();
        MySimpleLogger.info(loggerId, "Señal DESACTIVADA");
    }

    /**
     * Cerrar conexiones AWS + LOCAL
     */
    public void cerrar() {
        try {
            if (mqttClientPublisher != null && mqttClientPublisher.isConnected()) {
                mqttClientPublisher.disconnect();
                MySimpleLogger.info(loggerId, "MQTT LOCAL desconectado");
            }
            if (awsManager != null) {
                awsManager.disconnect();
                MySimpleLogger.info(loggerId, "AWS IoT desconectado");
            }
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error al cerrar conexiones: " + e.getMessage());
        }
    }

    // Métodos MqttCallback requeridos (para compatibilidad futura)
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // No suscribe, pero requerido por interfaz
    }

    @Override
    public void connectionLost(Throwable cause) {
        MySimpleLogger.error(loggerId, "Conexión LOCAL perdida: " + cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // No necesario para publisher-only
    }

    // Getters existentes
    public String getRoadSegment() { return roadSegment; }
    public String getId() { return id; }
    public int getVelocidadMaxima() { return velocidadMaxima; }
    public int getPosicionInicio() { return posicionInicio; }
    public int getPosicionFin() { return posicionFin; }
    public boolean isSeñalActiva() { return señalActiva; }
}
