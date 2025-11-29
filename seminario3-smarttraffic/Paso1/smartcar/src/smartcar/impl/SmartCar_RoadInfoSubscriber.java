package smartcar.impl;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

public class SmartCar_RoadInfoSubscriber implements MqttCallback {

	MqttClient myClient;
	MqttConnectOptions connOpt;

	static final String BROKER_URL = "tcp://tambori.dsic.upv.es:10083";
//	static final String M2MIO_USERNAME = "<m2m.io username>";
//	static final String M2MIO_PASSWORD_MD5 = "<m2m.io password (MD5 sum of password)>";

	SmartCar smartcar;
	
	public SmartCar_RoadInfoSubscriber(SmartCar smartcar) {
		this.smartcar = smartcar;
	}
	
	protected void _debug(String message) {
		System.out.println("(SmartCar: " + this.smartcar.getSmartCarID() + ") " + message);
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
		
		String payload = new String(message.getPayload());
		
		System.out.println("-------------------------------------------------");
		System.out.println("| Topic:" + topic);
		System.out.println("| Message: " + payload);
		System.out.println("-------------------------------------------------");
		
		// Procesar el mensaje JSON recibido
		try {
			JSONObject jsonMessage = new JSONObject(payload);
			
			// Extraer el tipo de mensaje
			// Primero intentamos con "type" (mensajes normales de info)
			// Si no existe, intentamos con "event" (mensajes de incidentes retransmitidos)
			String messageType = jsonMessage.optString("type", null);
			if (messageType == null) {
				// Si no hay "type", puede ser un mensaje de incidente retransmitido con "event"
				String event = jsonMessage.optString("event", null);
				if (event != null) {
					// Es un mensaje de incidente retransmitido por SmartRoad
					this._debug("INCIDENT ALERT received (retransmitted): " + event);
					String road = jsonMessage.optString("road", "UNKNOWN");
					int kp = jsonMessage.optInt("kp", -1);
					String vehicle = jsonMessage.optString("vehicle", "UNKNOWN");
					
					this._debug("Incident details - Road: " + road + ", Km: " + kp + ", Vehicle: " + vehicle + ", Event: " + event);
					
					// Si el incidente es en la misma carretera donde estamos, tomar acción
					if (road.equals(this.smartcar.getCurrentPlace().getRoad())) {
						this._debug("WARNING: Incident detected on current road! Taking evasive action.");
						// Aquí podrías implementar lógica para detenerse, cambiar de ruta, etc.
					}
					return; // Salir después de procesar el incidente
				}
				messageType = "UNKNOWN";
			}
			
			this._debug("Received message type: " + messageType);
			
			// Procesar según el tipo de mensaje
			switch (messageType.toUpperCase()) {
				case "SPEED_LIMIT":
					// Mensaje de límite de velocidad
					int speedLimit = jsonMessage.optInt("value", -1);
					long validity = jsonMessage.optLong("validity", -1);
					
					if (speedLimit > 0) {
						this._debug("SPEED_LIMIT detected: " + speedLimit + " km/h (validity: " + validity + "ms)");
						// Aquí podrías actualizar la velocidad del vehículo
						// Por ejemplo: this.smartcar.setMaxSpeed(speedLimit);
					}
					break;
					
				case "TRAFFIC_LIGHT":
					// Mensaje de semáforo
					String state = jsonMessage.optString("state", "UNKNOWN");
					this._debug("TRAFFIC_LIGHT detected: " + state);
					
					if ("RED".equalsIgnoreCase(state)) {
						this._debug("STOP! Red light detected.");
						// Aquí podrías implementar la lógica para detener el vehículo
					} else if ("GREEN".equalsIgnoreCase(state)) {
						this._debug("GO! Green light detected.");
					} else if ("YELLOW".equalsIgnoreCase(state)) {
						this._debug("CAUTION! Yellow light detected.");
					}
					break;
					
				case "CONGESTION":
					// Mensaje de congestión
					int congestionLevel = jsonMessage.optInt("level", -1);
					this._debug("CONGESTION detected: Level " + congestionLevel);
					// Aquí podrías ajustar la velocidad según la congestión
					break;
					
				case "INCIDENT":
					// Mensaje de incidente (formato directo)
					String incidentType = jsonMessage.optString("incidentType", "UNKNOWN");
					this._debug("INCIDENT detected: " + incidentType);
					// Notificar al SmartCar sobre el incidente
					this.smartcar.notifyIncident(incidentType);
					break;
					
				default:
					this._debug("Unknown message type: " + messageType);
					// Mostrar todos los campos del JSON para debugging
					this._debug("Full JSON content: " + jsonMessage.toString());
					break;
			}
			
		} catch (Exception e) {
			this._debug("Error parsing JSON message: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * runClient
	 * The main functionality of this simple example.
	 * Create a MQTT client, connect to broker, pub/sub, disconnect.
	 * 
	 */
	public void connect() {
		// setup MQTT Client
		String clientID = this.smartcar.getSmartCarID() + ".subscriber";
		connOpt = new MqttConnectOptions();
		
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
//			connOpt.setUserName(M2MIO_USERNAME);
//			connOpt.setPassword(M2MIO_PASSWORD_MD5.toCharArray());
		
		// Connect to Broker
		try {
			myClient = new MqttClient(BROKER_URL, clientID);
			myClient.setCallback(this);
			myClient.connect(connOpt);
		} catch (MqttException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		this._debug("Subscriber Connected to " + BROKER_URL);

	}
	
	
	public void disconnect() {
		
		// disconnect
		try {
			// wait to ensure subscribed messages are delivered
			Thread.sleep(120000);

			myClient.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}

		
	}

	
	public void subscribe(String myTopic) {
		
		// subscribe to topic
		try {
			int subQoS = 0;
			myClient.subscribe(myTopic, subQoS);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	

	public void unsubscribe(String myTopic) {
		
		// unsubscribe to topic
		try {
			myClient.unsubscribe(myTopic);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
