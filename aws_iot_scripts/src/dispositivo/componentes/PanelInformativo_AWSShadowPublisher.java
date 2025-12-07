package dispositivo.componentes;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;

import org.json.JSONObject;

import java.util.UUID;

import dispositivo.utils.MySimpleLogger;

/**
 * Publisher para AWS IoT Device Shadow del PanelInformativo.
 * Publica el estado del panel (funciones, contadores, etc.) en cada actualización.
 * 
 * Topic: $aws/things/{thingName}/shadow/update
 * Formato: { "state": { "reported": { ... } } }
 */
public class PanelInformativo_AWSShadowPublisher {

	private AWSIotMqttClient client = null;
	private String thingName = null;
	private PanelInformativo_aws panel = null;
	private String endpoint = null;
	private String certPath = null;
	private String keyPath = null;
	
	private static final AWSIotQos QOS = AWSIotQos.QOS0;
	
	public PanelInformativo_AWSShadowPublisher(PanelInformativo_aws panel, String thingName, 
	                                           String endpoint, String certPath, String keyPath) {
		this.panel = panel;
		this.thingName = thingName;
		this.endpoint = endpoint;
		this.certPath = certPath;
		this.keyPath = keyPath;
	}
	
	/**
	 * Conecta al cliente AWS IoT MQTT
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
			String clientId = "panel-shadow-pub-" + this.panel.getTtmiID() + "-" + UUID.randomUUID().toString();
			this.client = new AWSIotMqttClient(endpoint, clientId, pair.keyStore, pair.keyPassword);
			
			// Conectar
			this.client.connect();
			this._debug("Connected to AWS IoT: " + endpoint);
			
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
	 * Publica el estado actual del PanelInformativo en AWS IoT Device Shadow.
	 */
	public void publishState() {
		if (this.client == null) {
			this._debug("Client not initialized, skipping state publication");
			return;
		}
		
		try {
			// Construir mensaje de estado "reported"
			JSONObject reportedState = panel.buildReportedState();
			JSONObject shadowMessage = buildShadowUpdateMessage(reportedState);
			
			// Topic del Device Shadow
			String topic = "$aws/things/" + this.thingName + "/shadow/update";
			
			// Publicar
			AWSIotMessage message = new AWSIotMessage(topic, QOS, shadowMessage.toString());
			this.client.publish(message);
			
			this._debug("Published state to shadow: " + shadowMessage.toString());
			
		} catch (AWSIotException e) {
			this._error("Error publishing to AWS IoT Shadow: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			this._error("Error building shadow message: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Construye el mensaje completo para actualizar el Device Shadow.
	 * Formato: { "state": { "reported": { ... } } }
	 */
	private JSONObject buildShadowUpdateMessage(JSONObject reportedState) throws Exception {
		JSONObject shadowMessage = new JSONObject();
		JSONObject state = new JSONObject();
		state.put("reported", reportedState);
		shadowMessage.put("state", state);
		return shadowMessage;
	}
	
	private void _debug(String message) {
		MySimpleLogger.info(panel.getLoggerId(), "(AWSShadowPublisher) " + message);
	}
	
	private void _error(String message) {
		MySimpleLogger.error(panel.getLoggerId(), "(AWSShadowPublisher) " + message);
	}
	
	public boolean isConnected() {
		return this.client != null;
	}
}

