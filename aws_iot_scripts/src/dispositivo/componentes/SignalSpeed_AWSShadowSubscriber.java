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

import dispositivo.utils.MySimpleLogger;

/**
 * Subscriber para AWS IoT Device Shadow de SignalSpeed.
 * Escucha cambios en el estado "desired" del Device Shadow para recibir comandos.
 * 
 * Topics:
 * - $aws/things/{thingName}/shadow/update/delta - Recibe solo los cambios (delta)
 */
public class SignalSpeed_AWSShadowSubscriber extends AWSIotTopic {

	protected SignalSpeed_aws signal = null;
	protected String thingName = null;
	protected AWSIotMqttClient client = null;
	protected String endpoint = null;
	protected String certPath = null;
	protected String keyPath = null;
	
	private static final AWSIotQos QOS = AWSIotQos.QOS0;
	
	/**
	 * Constructor para el handler del topic delta
	 */
	public SignalSpeed_AWSShadowSubscriber(SignalSpeed_aws signal, String thingName, 
	                                       String endpoint, String certPath, String keyPath, String topic) {
		super(topic, QOS);
		this.signal = signal;
		this.thingName = thingName;
		this.endpoint = endpoint;
		this.certPath = certPath;
		this.keyPath = keyPath;
	}
	
	/**
	 * Conecta al cliente AWS IoT MQTT y se suscribe al topic delta
	 */
	public void connect() {
		try {
			java.io.File certFileObj = new java.io.File(certPath);
			java.io.File keyFileObj = new java.io.File(keyPath);
			
			if (!certFileObj.exists() || !keyFileObj.exists()) {
				this._error("Certificados no encontrados. Cert: " + certPath + ", Key: " + keyPath);
				return;
			}
			
			// Inicializar cliente AWS IoT
			KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certPath, keyPath);
			String clientId = "signal-shadow-sub-" + this.signal.getId() + "-" + UUID.randomUUID().toString();
			this.client = new AWSIotMqttClient(endpoint, clientId, pair.keyStore, pair.keyPassword);
			
			// Conectar
			this.client.connect();
			this._debug("Connected to AWS IoT: " + endpoint);
			
			// Suscribirse al topic delta (usar este mismo objeto como handler)
			String deltaTopic = "$aws/things/" + this.thingName + "/shadow/update/delta";
			// Crear un nuevo handler con el topic correcto
			SignalSpeed_AWSShadowSubscriber deltaHandler = new SignalSpeed_AWSShadowSubscriber(
				this.signal, this.thingName, this.endpoint, this.certPath, this.keyPath, deltaTopic);
			deltaHandler.client = this.client; // Compartir el mismo cliente
			this.client.subscribe(deltaHandler);
			this._debug("Subscribed to delta topic: " + deltaTopic);
			
		} catch (AWSIotException e) {
			this._error("Error connecting to AWS IoT: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			this._error("Error initializing AWS IoT client: " + e.getMessage());
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
		MySimpleLogger.info(signal.getLoggerId(), "(AWSShadowSubscriber) " + message);
	}
	
	private void _error(String message) {
		MySimpleLogger.error(signal.getLoggerId(), "(AWSShadowSubscriber) " + message);
	}
	
	public boolean isConnected() {
		return this.client != null;
	}
}

