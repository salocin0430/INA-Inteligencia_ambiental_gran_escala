package dispositivo.componentes;

import org.json.JSONObject;

import dispositivo.api.mqtt.Dispositivo_APIMQTT;


public class SignalSpeed{

	private String roadSegment;
	private String id;
    private int velocidadMaxima;
    private int posicionInicio;
    private int posicionFin;
    private Dispositivo_APIMQTT mqttClient;
    private String topicPublicacion;

    public SignalSpeed(String roadSegment, String id, int velocidadMaxima, int posicionInicio, int posicionFin,
                       Dispositivo_APIMQTT mqttClient, String topicPublicacion) {

        this.roadSegment = roadSegment;
		this.id = id;
        this.velocidadMaxima = velocidadMaxima;
        this.posicionInicio = posicionInicio;
        this.posicionFin = posicionFin;
        this.mqttClient = mqttClient;
        this.topicPublicacion = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + roadSegment + "/speed-limit";
    }



    // Forma y envía el mensaje JSON con el estado de la señal
	private void publicarEstado() {
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

		mqttClient.publish(topicPublicacion, root.toString());
	}

}
	
