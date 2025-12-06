package roadmanager.impl;

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
 * Publisher para publicar información en el canal info de los segmentos (simulador)
 * Retransmite alertas y publica señales de tráfico
 */
public class RoadManager_InfoPublisher implements MqttCallback {
	
	private MqttClient myClient;
	private MqttConnectOptions connOpt;
	
	static final String BROKER_URL = "tcp://tambori.dsic.upv.es:10083";
	static final String TOPIC_ROOT = "es/upv/pros/tatami/smartcities/traffic/PTPaterna";
	
	private RoadManager roadManager;
	
	public RoadManager_InfoPublisher(RoadManager roadManager) {
		this.roadManager = roadManager;
	}
	
	protected void _debug(String message) {
		System.out.println("(RoadManager InfoPublisher: " + this.roadManager.getManagerId() + ") " + message);
	}
	
	@Override
	public void connectionLost(Throwable cause) {
		this._debug("Connection lost: " + (cause != null ? cause.getMessage() : "Unknown"));
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// No aplica
	}
	
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// No aplica para publisher
	}
	
	/**
	 * Conecta al broker MQTT del simulador
	 */
	public void connect() {
		String clientID = "road-manager-" + this.roadManager.getManagerId() + "-info-pub";
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
			System.err.println("Error connecting info publisher: " + e.getMessage());
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
	 * Publica un mensaje de información (retransmisión de alerta) en el canal info
	 * @param roadSegment Segmento de carretera
	 * @param infoMessage Mensaje JSON a publicar
	 */
	public void publishInfo(String roadSegment, String infoMessage) {
		if (myClient == null || !myClient.isConnected()) {
			this._debug("Publisher not connected. Cannot publish.");
			return;
		}
		
		try {
			String topic = TOPIC_ROOT + "/road/" + roadSegment + "/info";
			MqttTopic mqttTopic = myClient.getTopic(topic);
			
			int pubQoS = 0;
			MqttMessage message = new MqttMessage(infoMessage.getBytes());
			message.setQos(pubQoS);
			message.setRetained(false);
			
			MqttDeliveryToken token = mqttTopic.publish(message);
			this._debug("Published info to topic [" + topic + "]: " + infoMessage);
			
		} catch (Exception e) {
			e.printStackTrace();
			this._debug("Error publishing info: " + e.getMessage());
		}
	}
	
	/**
	 * Publica una señal de límite de velocidad
	 * @param roadSegment Segmento de carretera
	 * @param signal Señal de límite de velocidad
	 */
	public void publishSpeedLimitSignal(String roadSegment, SpeedLimitSignal signal) {
		if (myClient == null || !myClient.isConnected()) {
			this._debug("Publisher not connected. Cannot publish speed-limit signal.");
			return;
		}
		
		try {
			// Construir mensaje de señal speed-limit
			JSONObject signalMessage = new JSONObject();
			signalMessage.put("type", "SPEED_LIMIT");
			signalMessage.put("value", signal.getValue());
			signalMessage.put("position-start", signal.getPositionStart());
			signalMessage.put("position-end", signal.getPositionEnd());
			signalMessage.put("validity", signal.getValidity());
			
			// Publicar en el topic de señales del segmento
			String topic = TOPIC_ROOT + "/road/" + roadSegment + "/signals";
			MqttTopic mqttTopic = myClient.getTopic(topic);
			
			int pubQoS = 0;
			MqttMessage message = new MqttMessage(signalMessage.toString().getBytes());
			message.setQos(pubQoS);
			message.setRetained(false);
			
			MqttDeliveryToken token = mqttTopic.publish(message);
			this._debug("Published speed-limit signal to topic [" + topic + "]: " + signalMessage.toString());
			
		} catch (Exception e) {
			e.printStackTrace();
			this._debug("Error publishing speed-limit signal: " + e.getMessage());
		}
	}
	
	/**
	 * Elimina la señal de límite de velocidad (publica señal con validez expirada)
	 * @param roadSegment Segmento de carretera
	 */
	public void removeSpeedLimitSignal(String roadSegment) {
		if (myClient == null || !myClient.isConnected()) {
			return;
		}
		
		try {
			// Publicar señal con validez 0 para indicar eliminación
			JSONObject signalMessage = new JSONObject();
			signalMessage.put("type", "SPEED_LIMIT");
			signalMessage.put("value", 0);
			signalMessage.put("position-start", 0);
			signalMessage.put("position-end", 0);
			signalMessage.put("validity", 0);
			
			String topic = TOPIC_ROOT + "/road/" + roadSegment + "/signals";
			MqttTopic mqttTopic = myClient.getTopic(topic);
			
			int pubQoS = 0;
			MqttMessage message = new MqttMessage(signalMessage.toString().getBytes());
			message.setQos(pubQoS);
			message.setRetained(false);
			
			MqttDeliveryToken token = mqttTopic.publish(message);
			this._debug("Removed speed-limit signal from topic [" + topic + "]");
			
		} catch (Exception e) {
			e.printStackTrace();
			this._debug("Error removing speed-limit signal: " + e.getMessage());
		}
	}
	
	public boolean isConnected() {
		return myClient != null && myClient.isConnected();
	}
}

