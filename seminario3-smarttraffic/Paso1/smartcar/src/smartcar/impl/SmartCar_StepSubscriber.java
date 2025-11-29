package smartcar.impl;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

/**
 * Suscriptor al topic 'step' del simulador.
 * Cada vez que el simulador publica un paso de simulación (cada 3 segundos),
 * este suscriptor notifica al SmartCar para que mueva su Navigator.
 */
public class SmartCar_StepSubscriber implements MqttCallback {

	private MqttClient myClient;
	private MqttConnectOptions connOpt;
	
	static final String BROKER_URL = "tcp://tambori.dsic.upv.es:10083";
	static final String STEP_TOPIC = "es/upv/pros/tatami/smartcities/traffic/PTPaterna/step";
	
	private SmartCar smartcar;
	
	public SmartCar_StepSubscriber(SmartCar smartcar) {
		this.smartcar = smartcar;
	}
	
	protected void _debug(String message) {
		System.out.println("(StepSubscriber: " + this.smartcar.getSmartCarID() + ") " + message);
	}
	
	/**
	 * Conecta al broker MQTT y se suscribe al topic 'step'
	 */
	public void connect() {
		String clientID = this.smartcar.getSmartCarID() + ".step-subscriber";
		connOpt = new MqttConnectOptions();
		
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
		
		try {
			myClient = new MqttClient(BROKER_URL, clientID);
			myClient.setCallback(this);
			myClient.connect(connOpt);
			this._debug("Connected to " + BROKER_URL);
			
			// Suscribirse al topic 'step'
			int subQoS = 0;
			myClient.subscribe(STEP_TOPIC, subQoS);
			this._debug("Subscribed to " + STEP_TOPIC);
			
		} catch (MqttException e) {
			e.printStackTrace();
			System.err.println("Error connecting step subscriber: " + e.getMessage());
		}
	}
	
	/**
	 * Desconecta del broker MQTT
	 */
	public void disconnect() {
		try {
			if (myClient != null && myClient.isConnected()) {
				myClient.unsubscribe(STEP_TOPIC);
				myClient.disconnect();
				this._debug("Disconnected");
			}
		} catch (MqttException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void connectionLost(Throwable cause) {
		this._debug("Connection lost: " + cause.getMessage());
		// Intentar reconectar
		try {
			connect();
		} catch (Exception e) {
			this._debug("Error reconnecting: " + e.getMessage());
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String payload = new String(message.getPayload());
		this._debug("Received step message: " + payload);
		
		try {
			// Parsear el mensaje JSON
			JSONObject jsonMessage = new JSONObject(payload);
			
			// Verificar que es un mensaje de tipo SIMULATOR_STEP
			String type = jsonMessage.optString("type", "");
			if (!"SIMULATOR_STEP".equals(type)) {
				this._debug("Ignoring message of type: " + type);
				return;
			}
			
			// Extraer el número de paso (opcional, para logging)
			JSONObject msg = jsonMessage.optJSONObject("msg");
			if (msg != null) {
				int step = msg.optInt("step", -1);
				this._debug("Processing simulation step: " + step);
			}
			
			// Notificar al SmartCar que debe moverse
			// El SmartCar calculará su velocidad y moverá el Navigator
			if (this.smartcar != null) {
				this.smartcar.onSimulationStep();
			}
			
		} catch (Exception e) {
			this._debug("Error processing step message: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// No aplica para suscripciones
	}
}

