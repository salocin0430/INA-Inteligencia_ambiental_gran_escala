package smartcar.impl;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

/**
 * Clase encargada de publicar mensajes de tráfico del SmartCar al broker MQTT.
 * Publica en el topic: es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road_segment}/traffic
 */
public class SmartCar_TrafficPublisher {

	private MqttClient myClient;
	private MqttConnectOptions connOpt;
	
	static final String BROKER_URL = "tcp://tambori.dsic.upv.es:10083";
	static final String TOPIC_ROOT = "es/upv/pros/tatami/smartcities/traffic/PTPaterna";
	
	private SmartCar smartcar;
	
	public SmartCar_TrafficPublisher(SmartCar smartcar) {
		this.smartcar = smartcar;
	}
	
	protected void _debug(String message) {
		System.out.println("(SmartCar Publisher: " + this.smartcar.getSmartCarID() + ") " + message);
	}
	
	/**
	 * Conecta al broker MQTT
	 */
	public void connect() {
		String clientID = this.smartcar.getSmartCarID() + ".publisher";
		connOpt = new MqttConnectOptions();
		
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
		
		try {
			myClient = new MqttClient(BROKER_URL, clientID);
			myClient.connect(connOpt);
			this._debug("Publisher Connected to " + BROKER_URL);
		} catch (MqttException e) {
			e.printStackTrace();
			System.err.println("Error connecting publisher: " + e.getMessage());
		}
	}
	
	/**
	 * Desconecta del broker MQTT
	 */
	public void disconnect() {
		try {
			if (myClient != null && myClient.isConnected()) {
				myClient.disconnect();
				this._debug("Publisher Disconnected");
			}
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Publica un mensaje VEHICLE_IN cuando el vehículo entra en un segmento de carretera
	 * @param roadSegment ID del segmento de carretera (ej: "R5s1")
	 * @param position Posición kilométrica en el segmento
	 */
	public void publishVehicleIn(String roadSegment, int position) {
		publishTrafficEvent(roadSegment, "VEHICLE_IN", position);
	}
	
	/**
	 * Publica un mensaje VEHICLE_OUT cuando el vehículo sale de un segmento de carretera
	 * @param roadSegment ID del segmento de carretera (ej: "R5s1")
	 * @param position Posición kilométrica en el segmento
	 */
	public void publishVehicleOut(String roadSegment, int position) {
		publishTrafficEvent(roadSegment, "VEHICLE_OUT", position);
	}
	
	/**
	 * Publica un evento de tráfico (VEHICLE_IN o VEHICLE_OUT) en el formato del proyecto práctico
	 * Formato según documento:
	 * {
	 *   "msg": {
	 *     "action": "VEHICLE_IN",
	 *     "vehicle-role": "PrivateUsage",
	 *     "vehicle-id": "3240JVM",
	 *     "road-segment": "R1s2a",
	 *     "position": 0
	 *   },
	 *   "id": "MSG_1638979846783",
	 *   "type": "TRAFFIC",
	 *   "timestamp": 1638979846783
	 * }
	 * 
	 * @param roadSegment ID del segmento de carretera
	 * @param action "VEHICLE_IN" o "VEHICLE_OUT"
	 * @param position Posición kilométrica en el segmento
	 */
	private void publishTrafficEvent(String roadSegment, String action, int position) {
		if (myClient == null || !myClient.isConnected()) {
			this._debug("Publisher not connected. Cannot publish.");
			return;
		}
		
		try {
			// Construir el topic: es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{roadSegment}/traffic
			String topic = TOPIC_ROOT + "/road/" + roadSegment + "/traffic";
			
			// Obtener el vehicle-role del SmartCar (por defecto "PrivateUsage")
			String vehicleRole = this.smartcar.getVehicleRole();
			if (vehicleRole == null || vehicleRole.isEmpty()) {
				vehicleRole = "PrivateUsage";
			}
			
			// Construir el payload JSON según el formato del proyecto
			long timestamp = System.currentTimeMillis();
			String messageId = "MSG_" + timestamp;
			
			JSONObject msg = new JSONObject();
			msg.put("action", action);
			msg.put("vehicle-role", vehicleRole);
			msg.put("vehicle-id", this.smartcar.getSmartCarID());
			msg.put("road-segment", roadSegment);
			msg.put("position", position);
			
			JSONObject payload = new JSONObject();
			payload.put("msg", msg);
			payload.put("id", messageId);
			payload.put("type", "TRAFFIC");
			payload.put("timestamp", timestamp);
			
			// Crear el mensaje MQTT
			MqttMessage message = new MqttMessage(payload.toString().getBytes());
			message.setQos(0);
			message.setRetained(false);
			
			// Publicar
			myClient.publish(topic, message);
			
			this._debug("Published to " + topic + ": " + payload.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Error publishing message: " + e.getMessage());
		}
	}
	
	// Métodos legacy para compatibilidad (deprecated)
	@Deprecated
	public void publishEnter(String roadSegment, int speed) {
		// Convertir a nuevo formato (asumiendo posición inicial 0)
		publishVehicleIn(roadSegment, 0);
	}
	
	@Deprecated
	public void publishExit(String roadSegment, int speed) {
		// Convertir a nuevo formato (asumiendo posición final)
		publishVehicleOut(roadSegment, 0);
	}
}

