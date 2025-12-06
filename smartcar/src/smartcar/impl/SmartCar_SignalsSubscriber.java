package smartcar.impl;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Suscriptor MQTT para recibir señales de tráfico (speed-limit, traffic-light)
 * del topic signals de cada segmento de carretera
 */
public class SmartCar_SignalsSubscriber implements MqttCallback {

	MqttClient myClient;
	MqttConnectOptions connOpt;

	static final String BROKER_URL = "tcp://tambori.dsic.upv.es:10083";
	static final String TOPIC_ROOT = "es/upv/pros/tatami/smartcities/traffic/PTPaterna";

	SmartCar smartcar;
	
	public SmartCar_SignalsSubscriber(SmartCar smartcar) {
		this.smartcar = smartcar;
	}
	
	protected void _debug(String message) {
		System.out.println("(SignalsSubscriber: " + this.smartcar.getSmartCarID() + ") " + message);
	}

	@Override
	public void connectionLost(Throwable t) {
		this._debug("Connection lost!");
		// code to reconnect to the broker would go here if desired
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// No usado en suscriptores
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String payload = new String(message.getPayload());
		this._debug("Received signal message from " + topic + ": " + payload);
		
		try {
			JSONObject jsonMessage = new JSONObject(payload);
			
			// Extraer el contenido del mensaje (puede estar en "msg" o directamente)
			JSONObject msgContent = jsonMessage.optJSONObject("msg");
			if (msgContent == null) {
				msgContent = jsonMessage; // Si no hay "msg", usar el JSON completo
			}
			
			String signalType = msgContent.optString("type", null);
			if (signalType == null) {
				this._debug("Signal message without type field");
				return;
			}
			
			// Extraer el segmento del topic: .../road/{segment}/signals
			String segment = extractSegmentFromTopic(topic);
			if (segment == null) {
				this._debug("Could not extract segment from topic: " + topic);
				return;
			}
			
			// Procesar según el tipo de señal
			switch (signalType.toUpperCase()) {
				case "SPEED_LIMIT":
					processSpeedLimitSignal(segment, msgContent);
					break;
					
				case "TRAFFIC_LIGHT":
					processTrafficLightSignal(segment, msgContent);
					break;
					
				default:
					this._debug("Unknown signal type: " + signalType);
					break;
			}
			
		} catch (Exception e) {
			this._debug("Error parsing signal message: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Procesa un mensaje de señal de límite de velocidad
	 */
	private void processSpeedLimitSignal(String segment, JSONObject msgContent) {
		try {
			int value = msgContent.getInt("value");
			int positionStart = msgContent.optInt("position-start", 0);
			int positionEnd = msgContent.optInt("position-end", Integer.MAX_VALUE);
			long validity = msgContent.optLong("validity", -1);
			
			SpeedLimitSignal signal = new SpeedLimitSignal(value, positionStart, positionEnd, validity);
			this.smartcar.addSpeedLimitSignal(segment, signal);
			this._debug("Added SPEED_LIMIT signal: " + signal);
			
		} catch (Exception e) {
			this._debug("Error processing SPEED_LIMIT signal: " + e.getMessage());
		}
	}
	
	/**
	 * Procesa un mensaje de señal de semáforo
	 */
	private void processTrafficLightSignal(String segment, JSONObject msgContent) {
		try {
			String state = msgContent.getString("state");
			int position = msgContent.getInt("position");
			
			TrafficLightSignal signal = new TrafficLightSignal(state, position);
			this.smartcar.addTrafficLightSignal(segment, signal);
			this._debug("Added TRAFFIC_LIGHT signal: " + signal);
			
		} catch (Exception e) {
			this._debug("Error processing TRAFFIC_LIGHT signal: " + e.getMessage());
		}
	}
	
	/**
	 * Extrae el nombre del segmento del topic MQTT
	 * Ejemplo: .../road/R5s1/signals -> R5s1
	 */
	private String extractSegmentFromTopic(String topic) {
		try {
			// El topic tiene formato: .../road/{segment}/signals
			String[] parts = topic.split("/");
			for (int i = 0; i < parts.length; i++) {
				if ("road".equals(parts[i]) && i + 1 < parts.length) {
					return parts[i + 1];
				}
			}
		} catch (Exception e) {
			// Ignorar
		}
		return null;
	}

	/**
	 * Conecta al broker MQTT
	 */
	public void connect() {
		String clientID = this.smartcar.getSmartCarID() + ".signals-subscriber";
		connOpt = new MqttConnectOptions();
		
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
		
		try {
			myClient = new MqttClient(BROKER_URL, clientID);
			myClient.setCallback(this);
			myClient.connect(connOpt);
			this._debug("Signals Subscriber Connected to " + BROKER_URL);
		} catch (MqttException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * Desconecta del broker MQTT
	 */
	public void disconnect() {
		try {
			if (myClient != null && myClient.isConnected()) {
				myClient.disconnect();
				this._debug("Signals Subscriber Disconnected");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Se suscribe al topic de señales de un segmento
	 */
	public void subscribe(String segment) {
		try {
			String topic = TOPIC_ROOT + "/road/" + segment + "/signals";
			int subQoS = 0;
			myClient.subscribe(topic, subQoS);
			this._debug("Subscribed to signals topic: " + topic);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Se desuscribe del topic de señales de un segmento
	 */
	public void unsubscribe(String segment) {
		try {
			String topic = TOPIC_ROOT + "/road/" + segment + "/signals";
			myClient.unsubscribe(topic);
			this._debug("Unsubscribed from signals topic: " + topic);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

