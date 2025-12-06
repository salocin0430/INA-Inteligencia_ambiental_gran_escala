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
			JSONObject msgContent = null;
			String messageType = null;
			
			// Detectar formato: puede ser con wrapper (msg, id, type, timestamp) o directo
			if (jsonMessage.has("msg")) {
				// Formato con wrapper
				msgContent = jsonMessage.getJSONObject("msg");
				messageType = jsonMessage.optString("type", null);
				this._debug("Message with wrapper format, type: " + messageType);
			} else {
				// Formato directo (sin wrapper)
				msgContent = jsonMessage;
				messageType = jsonMessage.optString("type", null);
				this._debug("Message without wrapper, type: " + messageType);
			}
			
			// Si no hay tipo en el nivel superior, buscar en el contenido del mensaje
			if (messageType == null && msgContent != null) {
				messageType = msgContent.optString("type", null);
			}
			
			// Si aún no hay tipo, puede ser un mensaje de incidente retransmitido con "event"
			if (messageType == null) {
				String event = null;
				if (msgContent != null) {
					event = msgContent.optString("event", null);
				}
				if (event == null) {
					event = jsonMessage.optString("event", null);
				}
				
				if (event != null) {
					// Es un mensaje de incidente retransmitido por SmartRoad
					this._debug("INCIDENT ALERT received (retransmitted): " + event);
					JSONObject source = msgContent != null ? msgContent : jsonMessage;
					String road = source.optString("road", source.optString("road-segment", "UNKNOWN"));
					int kp = source.optInt("kp", source.optInt("position", -1));
					String vehicle = source.optString("vehicle", source.optString("vehicle-id", "UNKNOWN"));
					
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
			
			// Usar msgContent si existe, sino usar jsonMessage
			JSONObject contentToProcess = msgContent != null ? msgContent : jsonMessage;
			
			this._debug("Received message type: " + messageType);
			
			// Procesar según el tipo de mensaje
			switch (messageType.toUpperCase()) {
				case "SPEED_LIMIT":
					// Mensaje de límite de velocidad
					int speedLimit = contentToProcess.optInt("value", -1);
					long validity = contentToProcess.optLong("validity", -1);
					
					if (speedLimit > 0) {
						this._debug("SPEED_LIMIT detected: " + speedLimit + " km/h (validity: " + validity + "ms)");
						// Aquí podrías actualizar la velocidad del vehículo
						// Por ejemplo: this.smartcar.setMaxSpeed(speedLimit);
					}
					break;
					
				case "TRAFFIC_LIGHT":
					// Mensaje de semáforo
					String state = contentToProcess.optString("state", "UNKNOWN");
					this._debug("TRAFFIC_LIGHT detected: " + state);
					
					if ("RED".equalsIgnoreCase(state) || "HLL".equalsIgnoreCase(state)) {
						this._debug("STOP! Red light detected.");
						// Aquí podrías implementar la lógica para detener el vehículo
					} else if ("GREEN".equalsIgnoreCase(state) || "LLH".equalsIgnoreCase(state)) {
						this._debug("GO! Green light detected.");
					} else if ("YELLOW".equalsIgnoreCase(state)) {
						this._debug("CAUTION! Yellow light detected.");
					}
					break;
					
				case "CONGESTION":
					// Mensaje de congestión
					int congestionLevel = contentToProcess.optInt("level", -1);
					this._debug("CONGESTION detected: Level " + congestionLevel);
					// Aquí podrías ajustar la velocidad según la congestión
					break;
					
				case "INCIDENT":
					// Mensaje de incidente (formato antiguo)
					String incidentType = contentToProcess.optString("incidentType", contentToProcess.optString("event", "UNKNOWN"));
					this._debug("INCIDENT detected: " + incidentType);
					// Notificar al SmartCar sobre el incidente
					this.smartcar.notifyIncident(incidentType);
					break;
					
				case "ACCIDENT":
					// Mensaje de accidente (formato nuevo con wrapper)
					String accidentEvent = contentToProcess.optString("event", "UNKNOWN"); // "OPEN" o "CLOSE"
					String accidentRt = contentToProcess.optString("rt", "accidente");
					String accidentId = contentToProcess.optString("id", "UNKNOWN");
					String accidentVehicle = contentToProcess.optString("vehicle", "UNKNOWN");
					String accidentRoad = contentToProcess.optString("road-segment", contentToProcess.optString("road", "UNKNOWN"));
					int accidentPosition = contentToProcess.optInt("position", contentToProcess.optInt("kp", -1));
					
					this._debug("ACCIDENT detected - Event: " + accidentEvent + ", Type: " + accidentRt + 
					           ", ID: " + accidentId + ", Vehicle: " + accidentVehicle + 
					           ", Road: " + accidentRoad + ", Position: " + accidentPosition);
					
					if ("OPEN".equalsIgnoreCase(accidentEvent)) {
						// Se abre un nuevo accidente
						this._debug("New accident opened: " + accidentRt);
						// Si el accidente es en la misma carretera donde estamos, tomar acción
						if (accidentRoad.equals(this.smartcar.getCurrentPlace().getRoad())) {
							this._debug("WARNING: Accident detected on current road! Taking evasive action.");
							// Aquí podrías implementar lógica para detenerse, cambiar de ruta, etc.
						}
					} else if ("CLOSE".equalsIgnoreCase(accidentEvent)) {
						// Se cierra un accidente
						this._debug("Accident closed: " + accidentId);
					}
					break;
					
				case "ROAD_STATUS":
					// Mensaje de estado de carretera (formato con wrapper)
					String roadSegment = contentToProcess.optString("road-segment", contentToProcess.optString("code", "UNKNOWN"));
					String status = contentToProcess.optString("status", "UNKNOWN");
					int numVehicles = contentToProcess.optInt("num-vehicles", -1);
					int maxSpeed = contentToProcess.optInt("max-speed", contentToProcess.optInt("current-max-speed", -1));
					
					this._debug("ROAD_STATUS received - Road: " + roadSegment + ", Status: " + status + 
					           ", Vehicles: " + numVehicles + ", Max Speed: " + maxSpeed);
					// Los carros no hacen mucho con esta información, solo la registran
					break;
					
				default:
					this._debug("Unknown message type: " + messageType);
					// Mostrar todos los campos del JSON para debugging
					this._debug("Full JSON content: " + (msgContent != null ? msgContent.toString() : jsonMessage.toString()));
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
