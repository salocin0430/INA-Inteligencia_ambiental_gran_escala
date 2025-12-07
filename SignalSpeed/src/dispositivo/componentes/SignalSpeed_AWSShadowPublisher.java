package dispositivo.componentes;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Publisher para AWS IoT Device Shadow de SignalSpeed.
 * Publica el estado de la señal de velocidad (activa/inactiva, velocidad máxima, etc.).
 * 
 * Topic: $aws/things/{thingName}/shadow/update
 * Formato: { "state": { "reported": { ... } } }
 */
public class SignalSpeed_AWSShadowPublisher {

	private AWSIotMqttClient client = null;
	private String thingName = null;
	private SignalSpeed signal = null;
	
	// Configuración AWS IoT
	private static final String CLIENT_ENDPOINT = "a7sfhuya0h87y-ats.iot.us-east-1.amazonaws.com";
	private static final String CERTS_DIR = "./certs/";
	
	// Certificados: puedes usar dispositivo2 (para múltiples Things) o certificados específicos
	// Opción 1: Usar certificado compartido (dispositivo2) - funciona para cualquier Thing
	private static final String CERTIFICATE_FILE_DEFAULT = CERTS_DIR + "dispositivo2-certificate.pem.crt";
	private static final String PRIVATE_KEY_FILE_DEFAULT = CERTS_DIR + "dispositivo2-private.pem.key";
	
	// Opción 2: Usar certificado específico del Thing (si existe)
	// El código intentará usar el certificado específico primero, si no existe, usa el default
	
	private static final AWSIotQos QOS = AWSIotQos.QOS0;
	
	public SignalSpeed_AWSShadowPublisher(SignalSpeed signal, String thingName) {
		this.signal = signal;
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
			String clientId = "signal-shadow-pub-" + this.signal.getId() + "-" + UUID.randomUUID().toString();
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
			this._error("Error disconnecting from AWS IoT: " + e.getMessage());
		}
	}
	
	/**
	 * Publica el estado actual de SignalSpeed en AWS IoT Device Shadow.
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
	 * Construye el objeto JSON con el estado "reported" de la señal.
	 */
	private JSONObject buildReportedState() throws JSONException {
		JSONObject state = new JSONObject();
		
		state.put("road_segment", this.signal.getRoadSegment());
		state.put("signal_id", this.signal.getId());
		state.put("speed_limit", this.signal.getVelocidadMaxima());
		state.put("position_start", this.signal.getPosicionInicio());
		state.put("position_end", this.signal.getPosicionFin());
		state.put("active", this.signal.isSeñalActiva());
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
		System.out.println("(AWSShadowPublisher: " + this.signal.getId() + ") " + message);
	}
	
	public boolean isConnected() {
		// AWSIotMqttClient no tiene método isConnected()
		// Simplemente verificamos que el cliente esté inicializado
		// La conexión se maneja con excepciones al publicar
		return this.client != null;
	}
}

