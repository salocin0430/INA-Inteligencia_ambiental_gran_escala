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

public class SignalSpeed_aws implements MqttCallback {
    
    private String roadSegment;
    private String id;
    private int velocidadMaxima;
    private int posicionInicio;
    private int posicionFin;
    private MqttClient mqttClientPublisher;
    private String topicPublicacion;
    private String loggerId;

    // AWS IoT components (siguiendo patrón de smartcar)
    private SignalSpeed_AWSShadowPublisher awsShadowPublisher = null;
    private SignalSpeed_AWSShadowSubscriber awsShadowSubscriber = null;
    private String awsEndpoint;
    private String awsThingName;
    private boolean señalActiva = false;

    public SignalSpeed_aws(String roadSegment, String id, int velocidadMaxima, int posicionInicio, int posicionFin,
                      String mqttBroker) {
        this.roadSegment = roadSegment;
        this.id = id;
        this.velocidadMaxima = velocidadMaxima;
        this.posicionInicio = posicionInicio;
        this.posicionFin = posicionFin;
        this.topicPublicacion = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + roadSegment + "/signals";
        this.loggerId = "SignalSpeed-" + id;

        try {
            this.mqttClientPublisher = new MqttClient(mqttBroker, "SignalSpeed_Pub_" + id, new MemoryPersistence());
            
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setKeepAliveInterval(60);
            
            this.mqttClientPublisher.connect(connOpts);
            MySimpleLogger.info(loggerId, "Conectado al broker MQTT LOCAL: " + mqttBroker);
            
        } catch (MqttException e) {
            MySimpleLogger.error(loggerId, "Error al conectar con MQTT LOCAL: " + e.getMessage());
            // No lanzamos excepción para permitir continuar
        }
    }

    /**
     * Inicializar AWS IoT (siguiendo patrón de smartcar)
     */
    public void initAWS(String endpoint, String thingName, String certPath, String keyPath, String caPath) {
        this.awsEndpoint = endpoint;
        this.awsThingName = thingName;
        
        try {
            // Device Shadow: Publicar estado
            if (this.awsShadowPublisher == null) {
                this.awsShadowPublisher = new SignalSpeed_AWSShadowPublisher(this, thingName, endpoint, certPath, keyPath);
                this.awsShadowPublisher.connect();
            }
            
            // Device Shadow: Recibir comandos
            if (this.awsShadowSubscriber == null) {
                String deltaTopic = "$aws/things/" + thingName + "/shadow/update/delta";
                this.awsShadowSubscriber = new SignalSpeed_AWSShadowSubscriber(this, thingName, endpoint, certPath, keyPath, deltaTopic);
                this.awsShadowSubscriber.connect();
            }
            
            MySimpleLogger.info(loggerId, "✓ AWS IoT inicializado para SignalSpeed: " + thingName);
            MySimpleLogger.info(loggerId, "  - Device Shadow: enabled");
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error AWS init SignalSpeed: " + e.getMessage());
            MySimpleLogger.warn(loggerId, "Continuando sin AWS IoT. Funcionalidad MQTT local seguirá activa.");
            this.awsShadowPublisher = null;
            this.awsShadowSubscriber = null;
        }
    }

    // Método para construir el estado reported (usado por AWSShadowPublisher)
    public JSONObject buildReportedState() {
        JSONObject state = new JSONObject();
        try {
            state.put("road_segment", roadSegment);
            state.put("signal_id", id);
            state.put("speed_limit", velocidadMaxima);
            state.put("position_start", posicionInicio);
            state.put("position_end", posicionFin);
            state.put("active", señalActiva);
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
            MySimpleLogger.info(loggerId, "Shadow SignalSpeed actualizado: " + velocidadMaxima + "km/h " + (señalActiva ? "ACTIVA" : "INACTIVA"));
        }
    }
    
    // Getters para las clases AWS
    public String getLoggerId() { return loggerId; }

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

            if (mqttClientPublisher != null && mqttClientPublisher.isConnected()) {
                MqttMessage message = new MqttMessage(payload.getBytes());
                message.setQos(1);
                message.setRetained(false);
                mqttClientPublisher.publish(topicPublicacion, message);
                MySimpleLogger.info(loggerId, "Mensaje LOCAL publicado en: " + topicPublicacion);
            }

            // Actualizar shadow cuando se publica estado
            if (this.awsShadowPublisher != null && this.awsShadowPublisher.isConnected()) {
                this.señalActiva = true;
                publishAwsState();
            }

        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error publicando mensaje: " + e.getMessage());
        }
    }

    public void activarSeñal() {
        this.señalActiva = true;
        publicarEstado();
        publishAwsState();
        MySimpleLogger.info(loggerId, "Señal ACTIVADA: " + velocidadMaxima + "km/h");
    }

    public void desactivarSeñal() {
        this.señalActiva = false;
        publishAwsState();
        MySimpleLogger.info(loggerId, "Señal DESACTIVADA");
    }

    public void cerrar() {
        try {
            if (mqttClientPublisher != null && mqttClientPublisher.isConnected()) {
                mqttClientPublisher.disconnect();
                MySimpleLogger.info(loggerId, "MQTT LOCAL desconectado");
            }
            if (this.awsShadowPublisher != null) {
                this.awsShadowPublisher.disconnect();
            }
            if (this.awsShadowSubscriber != null) {
                this.awsShadowSubscriber.disconnect();
            }
            MySimpleLogger.info(loggerId, "AWS IoT desconectado");
        } catch (Exception e) {
            MySimpleLogger.error(loggerId, "Error al cerrar conexiones: " + e.getMessage());
        }
    }

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

    public String getRoadSegment() { return roadSegment; }
    public String getId() { return id; }
    public int getVelocidadMaxima() { return velocidadMaxima; }
    public int getPosicionInicio() { return posicionInicio; }
    public int getPosicionFin() { return posicionFin; }
    public boolean isSeñalActiva() { return señalActiva; }
}