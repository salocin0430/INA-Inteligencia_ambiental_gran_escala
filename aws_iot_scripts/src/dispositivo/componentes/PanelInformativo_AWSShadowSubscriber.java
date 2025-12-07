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
 * Subscriber para AWS IoT Device Shadow del PanelInformativo.
 * Escucha cambios en el estado "desired" del Device Shadow para recibir comandos.
 * 
 * Topics:
 * - $aws/things/{thingName}/shadow/update/delta - Recibe solo los cambios (delta)
 */
public class PanelInformativo_AWSShadowSubscriber extends AWSIotTopic {

	protected PanelInformativo_aws panel = null;
	protected String thingName = null;
	protected AWSIotMqttClient client = null;
	protected String endpoint = null;
	protected String certPath = null;
	protected String keyPath = null;
	
	private static final AWSIotQos QOS = AWSIotQos.QOS0;
	
	/**
	 * Constructor para el handler del topic delta
	 */
	public PanelInformativo_AWSShadowSubscriber(PanelInformativo_aws panel, String thingName, 
	                                            String endpoint, String certPath, String keyPath, String topic) {
		super(topic, QOS);
		this.panel = panel;
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
			String clientId = "panel-shadow-sub-" + this.panel.getTtmiID() + "-" + UUID.randomUUID().toString();
			this.client = new AWSIotMqttClient(endpoint, clientId, pair.keyStore, pair.keyPassword);
			
			// Conectar
			this.client.connect();
			this._debug("Connected to AWS IoT: " + endpoint);
			
			// Suscribirse al topic delta (usar este mismo objeto como handler)
			String deltaTopic = "$aws/things/" + this.thingName + "/shadow/update/delta";
			// Crear un nuevo handler con el topic correcto
			PanelInformativo_AWSShadowSubscriber deltaHandler = new PanelInformativo_AWSShadowSubscriber(
				this.panel, this.thingName, this.endpoint, this.certPath, this.keyPath, deltaTopic);
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
		MySimpleLogger.info(panel.getLoggerId(), "(AWSShadowSubscriber) " + message);
	}
	
	private void _error(String message) {
		MySimpleLogger.error(panel.getLoggerId(), "(AWSShadowSubscriber) " + message);
	}
	
	public boolean isConnected() {
		return this.client != null;
	}
}

