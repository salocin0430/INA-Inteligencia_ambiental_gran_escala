package smartroad.impl;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

/**
 * Clase encargada de retransmitir alertas recibidas en el canal /alerts 
 * al canal /info de la carretera.
 */
public class SmartRoad_IncidentNotifier implements MqttCallback {

	private MqttClient myClient;
	private MqttConnectOptions connOpt;
	
	static final String BROKER_URL = "tcp://tambori.dsic.upv.es:10083";
	
	private SmartRoad road;
	
	public SmartRoad_IncidentNotifier(SmartRoad road) {
		this.road = road;
	}
	
	protected void _debug(String message) {
		System.out.println("(SmartRoad: " + this.road.getId() + ") " + message);
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
		// setup MQTT Client
		String clientID = this.road.getId() + ".publisher";
		connOpt = new MqttConnectOptions();
		
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
		
		// Connect to Broker
		try {
			myClient = new MqttClient(BROKER_URL, clientID);
			myClient.setCallback(this);
			myClient.connect(connOpt);
		} catch (MqttException e) {
			e.printStackTrace();
			System.err.println("Error connecting notifier: " + e.getMessage());
		}
		
		this._debug("Notifier Connected to " + BROKER_URL);
	}
	
	/**
	 * Desconecta del broker MQTT
	 */
	public void disconnect() {
		// disconnect
		try {
			if (myClient != null && myClient.isConnected()) {
				myClient.disconnect();
				this._debug("Notifier Disconnected");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Retransmite un mensaje recibido en /alerts al topic /info de la carretera
	 * @param infoMessage El mensaje JSON recibido en el canal alerts
	 */
	public void notify(String infoMessage) {
		if (myClient == null || !myClient.isConnected()) {
			this._debug("Notifier not connected. Cannot publish.");
			return;
		}
		
		String myTopic = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/" + this.road.getId() + "/info";
		MqttTopic topic = myClient.getTopic(myTopic);
		
		int pubQoS = 0;
		MqttMessage message = new MqttMessage(infoMessage.getBytes());
		message.setQos(pubQoS);
		message.setRetained(false);
		
		// Publish the message
		this._debug("Publishing to topic \"" + topic + "\" qos " + pubQoS);
		MqttDeliveryToken token = null;
		try {
			// publish message to broker
			token = topic.publish(message);
			this._debug(infoMessage);
			// Wait until the message has been delivered to the broker
			token.waitForCompletion();
			Thread.sleep(100);
		} catch (Exception e) {
			e.printStackTrace();
			this._debug("Error publishing message: " + e.getMessage());
		}
	}
}

