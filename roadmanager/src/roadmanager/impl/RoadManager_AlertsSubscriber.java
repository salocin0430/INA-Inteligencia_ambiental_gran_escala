package roadmanager.impl;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

/**
 * Suscriptor a alertas de todos los segmentos de carretera (simulador)
 * Retransmite las alertas recibidas al canal de información
 */
public class RoadManager_AlertsSubscriber implements MqttCallback {
	
	private MqttClient myClient;
	private MqttConnectOptions connOpt;
	
	static final String BROKER_URL = "tcp://tambori.dsic.upv.es:10083";
	
	private RoadManager roadManager;
	
	public RoadManager_AlertsSubscriber(RoadManager roadManager) {
		this.roadManager = roadManager;
	}
	
	protected void _debug(String message) {
		System.out.println("(RoadManager AlertsSubscriber: " + this.roadManager.getManagerId() + ") " + message);
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
		this._debug("Received alert from topic [" + topic + "]: " + payload);
		
		try {
			// Extraer el road-segment del topic
			// Formato: es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{road-segment}/alerts
			String[] topicParts = topic.split("/");
			String roadSegment = topicParts[topicParts.length - 2]; // Antes de "alerts"
			
			// También intentar extraer del mensaje JSON si está disponible
			try {
				JSONObject jsonMessage = new JSONObject(payload);
				if (jsonMessage.has("msg")) {
					JSONObject msg = jsonMessage.getJSONObject("msg");
					if (msg.has("road-segment")) {
						roadSegment = msg.getString("road-segment");
					} else if (msg.has("road")) {
						roadSegment = msg.getString("road");
					}
				}
			} catch (Exception e) {
				// Si no se puede parsear, usar el del topic
			}
			
			// Retransmitir la alerta al canal de información del mismo segmento
			this.roadManager.retransmitAlert(roadSegment, payload);
			
		} catch (Exception e) {
			this._debug("Error processing alert: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Conecta al broker MQTT del simulador
	 */
	public void connect() {
		String clientID = "road-manager-" + this.roadManager.getManagerId() + "-alerts-sub";
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
			System.err.println("Error connecting alerts subscriber: " + e.getMessage());
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

