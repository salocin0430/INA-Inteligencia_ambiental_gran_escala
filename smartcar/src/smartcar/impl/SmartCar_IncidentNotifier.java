package smartcar.impl;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.json.JSONObject;

/**
 * Clase encargada de notificar incidentes del SmartCar al broker MQTT.
 * Publica en el topic: es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road_segment}/alerts
 */
public class SmartCar_IncidentNotifier implements MqttCallback {

	private MqttClient myClient;
	private MqttConnectOptions connOpt;
	
	static final String BROKER_URL = "tcp://tambori.dsic.upv.es:10083";
	
	private String smartCarID;
	
	public SmartCar_IncidentNotifier(String smartCarID) {
		this.smartCarID = smartCarID;
	}
	
	protected void _debug(String message) {
		System.out.println("(SmartCar IncidentNotifier: " + this.smartCarID + ") " + message);
	}
	
	@Override
	public void connectionLost(Throwable t) {
		this._debug("Connection lost!");
		// code to reconnect to the broker would go here if desired
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		//System.out.println("Pub complete" + new String(token.getMessage().getPayload()));
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// Este callback no se usa en el notifier, pero es requerido por MqttCallback
	}
	
	/**
	 * Conecta al broker MQTT
	 */
	public void connect() {
		String clientID = this.smartCarID + ".incidentNotifier";
		connOpt = new MqttConnectOptions();
		
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
		
		try {
			myClient = new MqttClient(BROKER_URL, clientID);
			myClient.setCallback(this);
			myClient.connect(connOpt);
			this._debug("IncidentNotifier Connected to " + BROKER_URL);
		} catch (MqttException e) {
			e.printStackTrace();
			System.err.println("Error connecting incident notifier: " + e.getMessage());
		}
	}
	
	/**
	 * Desconecta del broker MQTT
	 */
	public void disconnect() {
		try {
			if (myClient != null && myClient.isConnected()) {
				myClient.disconnect();
				this._debug("IncidentNotifier Disconnected");
			}
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Publica un mensaje de alerta/incidente en el topic de alerts del segmento de carretera
	 * @param smartCarID ID del vehículo
	 * @param notificationType Tipo de notificación (ej: "accidente")
	 * @param place Ubicación del incidente (road y km)
	 */
	public void alert(String smartCarID, String notificationType, RoadPlace place) {
		if (myClient == null || !myClient.isConnected()) {
			this._debug("Notifier not connected. Cannot publish alert.");
			return;
		}
		
		// Construir el topic: es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road}/alerts
		String myTopic = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" 
				+ place.getRoad() + "/alerts";
		
		MqttTopic topic = myClient.getTopic(myTopic);
		
		// Construir el payload JSON con formato correcto del simulador
		// Formato: { "msg": { "event": "OPEN", "rt": "accidente", "id": ..., "vehicle": ..., "road-segment": ..., "position": ... }, "id": "MSG_...", "type": "ACCIDENT", "timestamp": ... }
		JSONObject msgContent = new JSONObject();
		JSONObject pubMsg = new JSONObject();
		try {
			// Determinar el tipo de evento (OPEN para abrir, CLOSE para cerrar)
			// Por defecto, cuando se notifica un incidente, se abre (OPEN)
			String eventType = "OPEN";
			String rt = notificationType; // rt puede ser "accidente", "avería", etc.
			
			// Generar ID único para el accidente
			String accidentId = "ACC_" + smartCarID + "_" + System.currentTimeMillis();
			
			msgContent.put("event", eventType);
			msgContent.put("rt", rt);
			msgContent.put("id", accidentId);
			msgContent.put("vehicle", smartCarID);
			msgContent.put("road-segment", place.getRoad());
			msgContent.put("road", place.getRoad()); // Compatibilidad
			msgContent.put("position", place.getKm());
			msgContent.put("kp", place.getKm()); // Compatibilidad
			
			pubMsg.put("msg", msgContent);
			pubMsg.put("id", "MSG_" + System.currentTimeMillis());
			pubMsg.put("type", "ACCIDENT");
			pubMsg.put("timestamp", System.currentTimeMillis());
		} catch (org.json.JSONException e1) {
			this._debug("Error creating JSON payload: " + e1.getMessage());
			return;
		}
		
		int pubQoS = 0;
		MqttMessage message = new MqttMessage(pubMsg.toString().getBytes());
		message.setQos(pubQoS);
		message.setRetained(false);
		
		// Publicar el mensaje
		MqttDeliveryToken token = null;
		try {
			// publish message to broker
			token = topic.publish(message);
			this._debug("Published alert to topic [" + myTopic + "]: " + pubMsg.toString());
			// No esperar completion ni hacer sleep para no bloquear el hilo principal
			// token.waitForCompletion();
		} catch (Exception e) {
			e.printStackTrace();
			this._debug("Error publishing alert: " + e.getMessage());
		}
	}
}

