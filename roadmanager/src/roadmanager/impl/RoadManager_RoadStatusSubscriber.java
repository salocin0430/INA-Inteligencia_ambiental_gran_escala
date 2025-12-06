package roadmanager.impl;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

/**
 * Suscriptor a mensajes ROAD_STATUS del canal info (simulador)
 * Procesa el estado de densidad de tráfico para auto-regulación de velocidad
 */
public class RoadManager_RoadStatusSubscriber implements MqttCallback {
	
	private MqttClient myClient;
	private MqttConnectOptions connOpt;
	
	static final String BROKER_URL = "tcp://tambori.dsic.upv.es:10083";
	
	private RoadManager roadManager;
	
	public RoadManager_RoadStatusSubscriber(RoadManager roadManager) {
		this.roadManager = roadManager;
	}
	
	protected void _debug(String message) {
		System.out.println("(RoadManager RoadStatusSubscriber: " + this.roadManager.getManagerId() + ") " + message);
	}
	
	@Override
	public void connectionLost(Throwable cause) {
		this._debug("Connection lost: " + (cause != null ? cause.getMessage() : "Unknown"));
		// Reconectar en hilo separado
		new Thread(() -> {
			try {
				Thread.sleep(1000);
				connect();
			} catch (Exception e) {
				this._debug("Error reconnecting: " + e.getMessage());
			}
		}).start();
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// No aplica para suscripciones
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String payload = new String(message.getPayload());
		
		try {
			// Extraer el road-segment del topic
			// Formato: es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road-segment}/info
			String[] topicParts = topic.split("/");
			String roadSegment = topicParts[topicParts.length - 2]; // Antes de "info"
			
			// Parsear el mensaje JSON
			JSONObject jsonMessage = new JSONObject(payload);
			JSONObject msgContent = null;
			String messageType = null;
			
			// Detectar formato: puede ser con wrapper o directo
			if (jsonMessage.has("msg")) {
				msgContent = jsonMessage.getJSONObject("msg");
				messageType = jsonMessage.optString("type", null);
			} else {
				msgContent = jsonMessage;
				messageType = jsonMessage.optString("type", null);
			}
			
			// Procesar solo mensajes ROAD_STATUS
			if ("ROAD_STATUS".equals(messageType) && msgContent != null) {
				String status = msgContent.optString("status", "UNKNOWN");
				int maxSpeed = msgContent.optInt("max-speed", msgContent.optInt("current-max-speed", 60));
				
				this._debug("ROAD_STATUS received for " + roadSegment + " - Status: " + status + ", Max Speed: " + maxSpeed);
				
				// Procesar el estado para auto-regulación de velocidad
				this.roadManager.processRoadStatus(roadSegment, status, maxSpeed);
			}
			
		} catch (Exception e) {
			this._debug("Error processing road status: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Conecta al broker MQTT del simulador
	 */
	public void connect() {
		String clientID = "road-manager-" + this.roadManager.getManagerId() + "-status-sub";
		connOpt = new MqttConnectOptions();
		
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(60);
		
		try {
			myClient = new MqttClient(BROKER_URL, clientID);
			myClient.setCallback(this);
			myClient.connect(connOpt);
			this._debug("Connected to " + BROKER_URL);
		} catch (MqttException e) {
			e.printStackTrace();
			System.err.println("Error connecting road status subscriber: " + e.getMessage());
		}
	}
	
	/**
	 * Desconecta del broker MQTT
	 */
	public void disconnect() {
		try {
			if (myClient != null && myClient.isConnected()) {
				myClient.disconnect();
				this._debug("Disconnected");
			}
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Suscribe a un topic (puede usar wildcards como + o #)
	 */
	public void subscribe(String topic) {
		try {
			if (myClient != null && myClient.isConnected()) {
				int subQoS = 0;
				myClient.subscribe(topic, subQoS);
				this._debug("Subscribed to: " + topic);
			}
		} catch (Exception e) {
			e.printStackTrace();
			this._debug("Error subscribing to " + topic + ": " + e.getMessage());
		}
	}
	
	public boolean isConnected() {
		return myClient != null && myClient.isConnected();
	}
}

