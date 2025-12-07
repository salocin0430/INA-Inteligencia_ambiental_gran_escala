package dispositivo.componentes;

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
 * Subscriber para AWS IoT Device Shadow de SignalSpeed.
 * Escucha cambios en el estado "desired" del Device Shadow para recibir comandos.
 * 
 * Topics:
 * - $aws/things/{thingName}/shadow/update/delta - Recibe solo los cambios (delta)
 */
public class SignalSpeed_AWSShadowSubscriber extends AWSIotTopic {

	protected SignalSpeed signal = null;
	protected String thingName = null;
	protected AWSIotMqttClient client = null;
	
	// Configuración AWS IoT
	private static final String CLIENT_ENDPOINT = "a7sfhuya0h87y-ats.iot.us-east-1.amazonaws.com";
	private static final String CERTS_DIR = "./certs/";
	private static final String CERTIFICATE_FILE_DEFAULT = CERTS_DIR + "dispositivo2-certificate.pem.crt";
	private static final String PRIVATE_KEY_FILE_DEFAULT = CERTS_DIR + "dispositivo2-private.pem.key";
	
	private static final AWSIotQos QOS = AWSIotQos.QOS0;
	
	/**
	 * Constructor para el handler del topic delta
	 */
	public SignalSpeed_AWSShadowSubscriber(SignalSpeed signal, String thingName, String topic) {
		super(topic, QOS);
		this.signal = signal;
		this.thingName = thingName;
	}
	
	/**
	 * Conecta al cliente AWS IoT MQTT y se suscribe al topic delta
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
			String clientId = "signal-shadow-sub-" + this.signal.getId() + "-" + UUID.randomUUID().toString();
			this.client = new AWSIotMqttClient(CLIENT_ENDPOINT, clientId, pair.keyStore, pair.keyPassword);
			
			// Conectar
			this.client.connect();
			this._debug("Connected to AWS IoT: " + CLIENT_ENDPOINT);
			
			// Suscribirse al topic delta (usar este mismo objeto como handler)
			String deltaTopic = "$aws/things/" + this.thingName + "/shadow/update/delta";
			// Crear un nuevo handler con el topic correcto
			SignalSpeed_AWSShadowSubscriber deltaHandler = new SignalSpeed_AWSShadowSubscriber(this.signal, this.thingName, deltaTopic);
			deltaHandler.client = this.client; // Compartir el mismo cliente
			this.client.subscribe(deltaHandler);
			this._debug("Subscribed to delta topic: " + deltaTopic);
			
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
	 * Método llamado automáticamente cuando se recibe un mensaje en el topic suscrito.
	 * Procesa los comandos del estado "desired" del Device Shadow.
	 */
	@Override
	public void onMessage(AWSIotMessage message) {
		String payload = message.getStringPayload();
		this._debug("Received shadow delta message: " + payload);
		
		try {
			JSONObject jsonMessage = new JSONObject(payload);
			
			// El mensaje delta tiene formato: { "state": { "activate": true, ... }, "version": 123, ... }
			JSONObject state = jsonMessage.optJSONObject("state");
			if (state == null) {
				this._debug("No 'state' field in delta message");
				return;
			}
			
			// Procesar cambios en el estado desired
			processDesiredState(state);
			
		} catch (Exception e) {
			this._error("Error processing shadow delta message: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Procesa los cambios en el estado "desired" y aplica los comandos a SignalSpeed.
	 * Comandos soportados:
	 * - activate: Activa o desactiva la señal (true/false)
	 */
	private void processDesiredState(JSONObject desiredState) {
		try {
			// Comando: activate
			if (desiredState.has("activate")) {
				boolean activate = desiredState.getBoolean("activate");
				if (activate) {
					signal.activarSeñal();
					this._debug("Received command to activate signal");
				} else {
					signal.desactivarSeñal();
					this._debug("Received command to deactivate signal");
				}
			}
			
			// Notificar al signal que actualice el estado
			signal.notifyStateChanged();
			
		} catch (Exception e) {
			this._error("Error processing desired state: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void _debug(String message) {
		System.out.println("(AWSShadowSubscriber: " + this.signal.getId() + ") " + message);
	}
	
	private void _error(String message) {
		System.err.println("(AWSShadowSubscriber: " + this.signal.getId() + ") ERROR: " + message);
	}
	
	public boolean isConnected() {
		// AWSIotMqttClient no tiene método isConnected()
		// Simplemente verificamos que el cliente esté inicializado
		// La conexión se maneja con excepciones al suscribirse
		return this.client != null;
	}
}

