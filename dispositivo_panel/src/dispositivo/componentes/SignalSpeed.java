package dispositivo.componentes;

import org.json.JSONObject;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;


import dispositivo.utils.MySimpleLogger;


public class SignalSpeed{

	private String roadSegment;
	private String id;
    private int velocidadMaxima;
    private int posicionInicio;
    private int posicionFin;
	private MqttClient mqttClientPublisher;   // Para publicar
    private String topicPublicacion;
    private String loggerId;

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
            
            // Crear cliente MQTT para publicaciones (enviar)
            this.mqttClientPublisher = new MqttClient(mqttBroker, "SignalSpeed_Pub", new MemoryPersistence());
            
            // Configurar opciones de conexión
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setKeepAliveInterval(60);
            
            this.mqttClientPublisher.connect(connOpts);
            MySimpleLogger.info(loggerId, "Conectado al broker MQTT: " + mqttBroker);
            
        } catch (MqttException e) {
            MySimpleLogger.error(loggerId, "Error al conectar con MQTT: " + e.getMessage());
            throw new RuntimeException("No se pudo conectar al broker MQTT", e);
        }

    }



    // Forma y envía el mensaje JSON con el estado de la señal
	public void publicarEstado() {

        try {
            JSONObject root = new JSONObject();
            JSONObject msg = new JSONObject();

            msg.put("signal-type", "SPEED_LIMIT");
            msg.put("rt", "traffic-signal");
            msg.put("id", this.id);  // Suponiendo que tienes un método o atributo que devuelve el ID de la señal
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
            message.setQos(1);           // Calidad de servicio (puedes usar 0,1 o 2 según tu necesidad)
            message.setRetained(false);  // Si quieres que el broker retenga el último mensaje en el topic

            mqttClientPublisher.publish(topicPublicacion, message);
            MySimpleLogger.info(id, "Mensaje publicado en topic " + topicPublicacion);

        } catch (Exception e) {
            MySimpleLogger.error(id, "Error publicando mensaje MQTT: " + e.getMessage());
            e.printStackTrace();
        }

}
}
