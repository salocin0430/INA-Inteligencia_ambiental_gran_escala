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
 * Subscriber para AWS IoT Device Shadow del PanelInformativo.
 * Escucha cambios en el estado "desired" del Device Shadow para recibir comandos.
 * 
 * Topics:
 * - $aws/things/{thingName}/shadow/update/delta - Recibe solo los cambios (delta)
 */
public class PanelInformativo_AWSShadowSubscriber extends AWSIotTopic {

	protected PanelInformativo panel = null;
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
	public PanelInformativo_AWSShadowSubscriber(PanelInformativo panel, String thingName, String topic) {
		super(topic, QOS);
		this.panel = panel;
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
			String clientId = "panel-shadow-sub-" + this.panel.getTtmiID() + "-" + UUID.randomUUID().toString();
			this.client = new AWSIotMqttClient(CLIENT_ENDPOINT, clientId, pair.keyStore, pair.keyPassword);
			
			// Conectar
			this.client.connect();
			this._debug("Connected to AWS IoT: " + CLIENT_ENDPOINT);
			
			// Suscribirse al topic delta (usar este mismo objeto como handler)
			String deltaTopic = "$aws/things/" + this.thingName + "/shadow/update/delta";
			// Crear un nuevo handler con el topic correcto
			PanelInformativo_AWSShadowSubscriber deltaHandler = new PanelInformativo_AWSShadowSubscriber(this.panel, this.thingName, deltaTopic);
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
			
			// El mensaje delta tiene formato: { "state": { "f1": "on", "f2": "off", ... }, "version": 123, ... }
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
	 * Procesa los cambios en el estado "desired" y aplica los comandos al PanelInformativo.
	 * Comandos soportados:
	 * - f1, f2, f3: Comandos para las funciones del semáforo (on, off, blink)
	 */
	private void processDesiredState(JSONObject desiredState) {
		try {
			// Comando: f1
			if (desiredState.has("f1")) {
				String accionF1 = desiredState.getString("f1");
				panel.aplicarComandoF1(accionF1);
				this._debug("Received command f1: " + accionF1);
			}
			
			// Comando: f2
			if (desiredState.has("f2")) {
				String accionF2 = desiredState.getString("f2");
				panel.aplicarComandoF2(accionF2);
				this._debug("Received command f2: " + accionF2);
			}
			
			// Comando: f3
			if (desiredState.has("f3")) {
				String accionF3 = desiredState.getString("f3");
				panel.aplicarComandoF3(accionF3);
				this._debug("Received command f3: " + accionF3);
			}
			
			// Notificar al panel que actualice el estado
			panel.notifyStateChanged();
			
		} catch (Exception e) {
			this._error("Error processing desired state: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void _debug(String message) {
		System.out.println("(AWSShadowSubscriber: " + this.panel.getTtmiID() + ") " + message);
	}
	
	private void _error(String message) {
		System.err.println("(AWSShadowSubscriber: " + this.panel.getTtmiID() + ") ERROR: " + message);
	}
	
	public boolean isConnected() {
		// AWSIotMqttClient no tiene método isConnected()
		// Simplemente verificamos que el cliente esté inicializado
		// La conexión se maneja con excepciones al suscribirse
		return this.client != null;
	}
}

