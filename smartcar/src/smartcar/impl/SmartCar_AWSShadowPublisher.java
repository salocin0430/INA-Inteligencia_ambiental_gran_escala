package smartcar.impl;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;

import ina.vehicle.navigation.interfaces.INavigator;
import ina.vehicle.navigation.interfaces.IRoadPoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Publisher para AWS IoT Device Shadow.
 * Publica el estado del SmartCar (ubicación, velocidad, destino) en cada paso de simulación.
 * 
 * Topic: $aws/things/{thingName}/shadow/update
 * Formato: { "state": { "reported": { ... } } }
 */
public class SmartCar_AWSShadowPublisher {

	private AWSIotMqttClient client = null;
	private String thingName = null; // Nombre del "thing" en AWS IoT
	private SmartCar smartcar = null;
	
	// Configuración AWS IoT
	private static final String CLIENT_ENDPOINT = "a1knxlrh9s811y-ats.iot.us-east-1.amazonaws.com";
	private static final String CERTS_DIR = "./certs/";
	
	// Certificados: puedes usar dispositivo2 (para múltiples Things) o certificados específicos
	// Opción 1: Usar certificado compartido (dispositivo2) - funciona para cualquier Thing
	private static final String CERTIFICATE_FILE_DEFAULT = CERTS_DIR + "dispositivo2-certificate.pem.crt";
	private static final String PRIVATE_KEY_FILE_DEFAULT = CERTS_DIR + "dispositivo2-private.pem.key";
	
	// Opción 2: Usar certificado específico del Thing (si existe)
	// El código intentará usar el certificado específico primero, si no existe, usa el default
	
	private static final AWSIotQos QOS = AWSIotQos.QOS0;
	
	public SmartCar_AWSShadowPublisher(SmartCar smartcar, String thingName) {
		this.smartcar = smartcar;
		this.thingName = thingName;
	}
	
	/**
	 * Conecta al cliente AWS IoT MQTT
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
			String clientId = "smartcar-" + this.smartcar.getSmartCarID() + "-" + UUID.randomUUID().toString();
			this.client = new AWSIotMqttClient(CLIENT_ENDPOINT, clientId, pair.keyStore, pair.keyPassword);
			
			// Conectar
			this.client.connect();
			this._debug("Connected to AWS IoT: " + CLIENT_ENDPOINT);
			
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
	 * Publica el estado actual del SmartCar en AWS IoT Device Shadow.
	 * Se debe llamar en cada paso de simulación.
	 */
	public void publishState() {
		if (this.client == null) {
			this._debug("Client not initialized, skipping state publication");
			return;
		}
		
		try {
			// Construir mensaje de estado "reported"
			JSONObject reportedState = buildReportedState();
			JSONObject shadowMessage = buildShadowUpdateMessage(reportedState);
			
			// Topic del Device Shadow
			String topic = "$aws/things/" + this.thingName + "/shadow/update";
			
			// Publicar
			AWSIotMessage message = new AWSIotMessage(topic, QOS, shadowMessage.toString());
			this.client.publish(message);
			
			this._debug("Published state to shadow: " + shadowMessage.toString());
			
		} catch (AWSIotException e) {
			System.err.println("Error publishing to AWS IoT Shadow: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Error building shadow message: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Construye el objeto JSON con el estado "reported" del vehículo.
	 * Incluye: ubicación (segmento y posición), velocidad actual, velocidad de crucero, destino.
	 */
	private JSONObject buildReportedState() throws JSONException {
		JSONObject state = new JSONObject();
		
		// Ubicación actual
		INavigator nav = this.smartcar.getNavigator();
		if (nav != null && nav.isRouting()) {
			IRoadPoint currentPos = nav.getCurrentPosition();
			if (currentPos != null) {
				JSONObject location = new JSONObject();
				location.put("road-segment", currentPos.getRoadSegment());
				location.put("position", currentPos.getPosition());
				state.put("location", location);
				
				// Destino
				IRoadPoint destination = nav.getDestinationPoint();
				if (destination != null) {
					JSONObject dest = new JSONObject();
					dest.put("road-segment", destination.getRoadSegment());
					dest.put("position", destination.getPosition());
					state.put("destination", dest);
				}
			}
		} else {
			// Si no hay Navigator, usar información básica
			JSONObject location = new JSONObject();
			location.put("road-segment", this.smartcar.getCurrentRoadSegment());
			location.put("position", this.smartcar.getCurrentPosition());
			state.put("location", location);
		}
		
		// Velocidad actual (calculada)
		int currentSpeed = this.smartcar.getCurrentSpeed();
		state.put("current-speed", currentSpeed);
		
		// Velocidad de crucero
		state.put("cruiser-speed", this.smartcar.getCruiserSpeed());
		
		// Rol del vehículo
		state.put("vehicle-role", this.smartcar.getVehicleRole());
		
		// Estado del Navigator
		if (nav != null) {
			state.put("navigator-status", nav.getNavigatorStatus().toString());
		}
		
		// Timestamp
		state.put("timestamp", System.currentTimeMillis());
		
		return state;
	}
	
	/**
	 * Construye el mensaje completo para actualizar el Device Shadow.
	 * Formato: { "state": { "reported": { ... } } }
	 */
	private JSONObject buildShadowUpdateMessage(JSONObject reportedState) throws JSONException {
		JSONObject shadowMessage = new JSONObject();
		JSONObject state = new JSONObject();
		state.put("reported", reportedState);
		shadowMessage.put("state", state);
		return shadowMessage;
	}
	
	private void _debug(String message) {
		System.out.println("(AWSShadowPublisher: " + this.smartcar.getSmartCarID() + ") " + message);
	}
	
	public boolean isConnected() {
		// AWSIotMqttClient no tiene método isConnected()
		// Simplemente verificamos que el cliente esté inicializado
		// La conexión se maneja con excepciones al publicar
		return this.client != null;
	}
}

