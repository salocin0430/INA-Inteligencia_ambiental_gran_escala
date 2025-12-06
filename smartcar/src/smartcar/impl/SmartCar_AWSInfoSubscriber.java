package smartcar.impl;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Subscriber para escuchar el canal 'info' en AWS IoT.
 * Escucha información y alertas publicadas en AWS IoT para el SmartCar.
 * 
 * Topic: dispositivo/{thingName}/info
 */
public class SmartCar_AWSInfoSubscriber extends AWSIotTopic {

	protected SmartCar smartcar = null;
	protected String thingName = null;
	protected AWSIotMqttClient client = null;
	
	// Configuración AWS IoT
	private static final String CLIENT_ENDPOINT = "a1knxlrh9s811y-ats.iot.us-east-1.amazonaws.com";
	private static final String CERTS_DIR = "./certs/";
	private static final String CERTIFICATE_FILE_DEFAULT = CERTS_DIR + "dispositivo2-certificate.pem.crt";
	private static final String PRIVATE_KEY_FILE_DEFAULT = CERTS_DIR + "dispositivo2-private.pem.key";
	
	private static final AWSIotQos QOS = AWSIotQos.QOS0;
	
	/**
	 * Constructor para el handler del topic info
	 */
	public SmartCar_AWSInfoSubscriber(SmartCar smartcar, String thingName, String topic) {
		super(topic, QOS);
		this.smartcar = smartcar;
		this.thingName = thingName;
	}
	
	/**
	 * Conecta al cliente AWS IoT MQTT y se suscribe al topic info
	 */
	public void connect() {
		try {
			// Intentar usar certificado específico del Thing, si no existe, usar el default
			String certFile = CERTS_DIR + this.thingName + "-certificate.pem.crt";
			String keyFile = CERTS_DIR + this.thingName + "-private.pem.key";
			
			java.io.File certFileObj = new java.io.File(certFile);
			java.io.File keyFileObj = new java.io.File(keyFile);
			
			// Si no existe el certificado específico, usar el default (dispositivo2)
			if (!certFileObj.exists() || !keyFileObj.exists()) {
				certFile = CERTIFICATE_FILE_DEFAULT;
				keyFile = PRIVATE_KEY_FILE_DEFAULT;
				this._debug("Using default certificate (dispositivo2) for Thing: " + this.thingName);
			} else {
				this._debug("Using specific certificate for Thing: " + this.thingName);
			}
			
			// Inicializar cliente AWS IoT
			KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certFile, keyFile);
			String clientId = "smartcar-info-" + this.smartcar.getSmartCarID() + "-" + UUID.randomUUID().toString();
			this.client = new AWSIotMqttClient(CLIENT_ENDPOINT, clientId, pair.keyStore, pair.keyPassword);
			
			// Conectar
			this.client.connect();
			this._debug("Connected to AWS IoT: " + CLIENT_ENDPOINT);
			
			// Suscribirse al topic info del segmento actual (si existe)
			// La suscripción se actualizará dinámicamente cuando cambie de segmento
			String currentSegment = this.smartcar.getCurrentRoadSegment();
			if (currentSegment != null) {
				this.subscribeToSegment(currentSegment);
			}
			
		} catch (AWSIotException e) {
			System.err.println("Error connecting to AWS IoT: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Error initializing AWS IoT client: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Desconecta del cliente AWS IoT MQTT
	 */
	public void disconnect() {
		try {
			if (this.client != null) {
				this.client.disconnect();
				this._debug("Disconnected from AWS IoT");
			}
		} catch (AWSIotException e) {
			System.err.println("Error disconnecting from AWS IoT: " + e.getMessage());
		}
	}
	
	/**
	 * Suscribe al topic info de un segmento específico
	 */
	public void subscribeToSegment(String roadSegment) {
		if (this.client == null) {
			return;
		}
		
		try {
			// Topic del formato del simulador: es/upv/pros/tatami/smartcities/traffic/PTPaterna/road/{segment}/info
			String infoTopic = "smartcities/traffic/PTPaterna/road/" + roadSegment + "/info";
			
			// Crear handler para este topic
			SmartCar_AWSInfoSubscriber infoHandler = new SmartCar_AWSInfoSubscriber(this.smartcar, this.thingName, infoTopic);
			infoHandler.client = this.client; // Compartir el mismo cliente
			this.client.subscribe(infoHandler);
			this._debug("Subscribed to info topic: " + infoTopic);
			
		} catch (AWSIotException e) {
			System.err.println("Error subscribing to info topic: " + e.getMessage());
		}
	}
	
	/**
	 * Método llamado automáticamente cuando se recibe un mensaje en el topic suscrito.
	 * Procesa mensajes de información y alertas del canal 'info' en AWS IoT.
	 * Soporta formato con wrapper (msg, id, type, timestamp) y formato simple.
	 */
	@Override
	public void onMessage(AWSIotMessage message) {
		String payload = message.getStringPayload();
		this._debug("Received info message from AWS IoT: " + payload);
		
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
				if (messageType == null) {
					messageType = jsonMessage.optString("event", null);
				}
				this._debug("Message without wrapper, type: " + messageType);
			}
			
			// Procesar según el tipo de mensaje
			if (messageType != null) {
				switch (messageType.toUpperCase()) {
					case "ALERT":
					case "INCIDENT":
						processAlertMessage(msgContent != null ? msgContent : jsonMessage);
						break;
						
					case "ACCIDENT":
						// Mensaje de accidente (formato nuevo con wrapper)
						processAccidentMessage(msgContent != null ? msgContent : jsonMessage);
						break;
						
					case "ROAD_STATUS":
					case "INFO":
					case "INFORMATION":
						processInfoMessage(msgContent != null ? msgContent : jsonMessage);
						break;
						
					default:
						this._debug("Unknown message type: " + messageType);
						// Procesar como mensaje genérico (puede ser ROAD_STATUS u otro)
						processInfoMessage(msgContent != null ? msgContent : jsonMessage);
						break;
				}
			} else {
				// Si no hay tipo, procesar como mensaje genérico
				processInfoMessage(msgContent != null ? msgContent : jsonMessage);
			}
			
		} catch (Exception e) {
			System.err.println("Error processing info message: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Procesa un mensaje de alerta/incidente
	 */
	private void processAlertMessage(JSONObject message) {
		try {
			String event = message.optString("event", message.optString("incidentType", "UNKNOWN"));
			String roadSegment = message.optString("road-segment", message.optString("road", message.optString("code", "UNKNOWN")));
			int position = message.optInt("position", message.optInt("kp", -1));
			
			this._debug("ALERT received from AWS IoT - Event: " + event + ", Road: " + roadSegment + ", Position: " + position);
			
			// Si el incidente es en la misma carretera donde estamos, tomar acción
			if (roadSegment.equals(this.smartcar.getCurrentRoadSegment())) {
				this._debug("WARNING: Alert detected on current road segment! Taking evasive action.");
				// Los carros no hacen mucho con esto, solo registran
			}
			
		} catch (Exception e) {
			System.err.println("Error processing alert message: " + e.getMessage());
		}
	}
	
	/**
	 * Procesa un mensaje de accidente 
	 */
	private void processAccidentMessage(JSONObject message) {
		try {
			String accidentEvent = message.optString("event", "UNKNOWN"); // "OPEN" o "CLOSE"
			String accidentRt = message.optString("rt", "accidente");
			String accidentId = message.optString("id", "UNKNOWN");
			String accidentVehicle = message.optString("vehicle", "UNKNOWN");
			String accidentRoad = message.optString("road-segment", message.optString("road", "UNKNOWN"));
			int accidentPosition = message.optInt("position", message.optInt("kp", -1));
			
			this._debug("ACCIDENT received from AWS IoT - Event: " + accidentEvent + ", Type: " + accidentRt + 
			           ", ID: " + accidentId + ", Vehicle: " + accidentVehicle + 
			           ", Road: " + accidentRoad + ", Position: " + accidentPosition);
			
			if ("OPEN".equalsIgnoreCase(accidentEvent)) {
				// Se abre un nuevo accidente
				this._debug("New accident opened: " + accidentRt);
				// Si el accidente es en la misma carretera donde estamos, tomar acción
				if (accidentRoad.equals(this.smartcar.getCurrentRoadSegment())) {
					this._debug("WARNING: Accident detected on current road segment! Taking evasive action.");
					// Los carros no hacen mucho con esto, solo registran
				}
			} else if ("CLOSE".equalsIgnoreCase(accidentEvent)) {
				// Se cierra un accidente
				this._debug("Accident closed: " + accidentId);
			}
			
		} catch (Exception e) {
			System.err.println("Error processing accident message: " + e.getMessage());
		}
	}
	
	/**
	 * Procesa un mensaje de información genérico (ROAD_STATUS, etc.)
	 * Los carros no hacen mucho con esta información, solo la registran
	 */
	private void processInfoMessage(JSONObject message) {
		try {
			// Detectar tipo de mensaje
			String roadSegment = message.optString("road-segment", message.optString("code", "UNKNOWN"));
			String status = message.optString("status", "UNKNOWN");
			int numVehicles = message.optInt("num-vehicles", -1);
			int maxSpeed = message.optInt("max-speed", message.optInt("current-max-speed", -1));
			
			this._debug("INFO message received from AWS IoT - Road: " + roadSegment + ", Status: " + status + 
			           ", Vehicles: " + numVehicles + ", Max Speed: " + maxSpeed);
			
			// Los carros no hacen mucho con esta información, solo la registran
			// Podría usarse para ajustar comportamiento futuro, pero por ahora solo se registra
			
		} catch (Exception e) {
			System.err.println("Error processing info message: " + e.getMessage());
		}
	}
	
	private void _debug(String message) {
		System.out.println("(AWSInfoSubscriber: " + this.smartcar.getSmartCarID() + ") " + message);
	}
	
	public boolean isConnected() {
		return this.client != null;
	}
}

