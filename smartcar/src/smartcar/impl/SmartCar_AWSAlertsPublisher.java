package smartcar.impl;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Publisher para publicar alertas/incidentes en AWS IoT.
 * Publica en el topic: dispositivo/{thingName}/alerts
 * 
 * Complementa SmartCar_IncidentNotifier que publica en MQTT del simulador.
 */
public class SmartCar_AWSAlertsPublisher {

	private AWSIotMqttClient client = null;
	private String thingName = null;
	private SmartCar smartcar = null;
	
	// Configuración AWS IoT
	private static final String CLIENT_ENDPOINT = "a1knxlrh9s811y-ats.iot.us-east-1.amazonaws.com";
	private static final String CERTS_DIR = "./certs/";
	private static final String CERTIFICATE_FILE_DEFAULT = CERTS_DIR + "dispositivo2-certificate.pem.crt";
	private static final String PRIVATE_KEY_FILE_DEFAULT = CERTS_DIR + "dispositivo2-private.pem.key";
	
	private static final AWSIotQos QOS = AWSIotQos.QOS0;
	
	public SmartCar_AWSAlertsPublisher(SmartCar smartcar, String thingName) {
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
			String clientId = "smartcar-alerts-" + this.smartcar.getSmartCarID() + "-" + UUID.randomUUID().toString();
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
	 * Publica una alerta/incidente en AWS IoT.
	 * @param incidentType Tipo de incidente (ej: "accidente", "avería", etc.)
	 * @param roadSegment Segmento de carretera donde ocurrió
	 * @param position Posición (km) donde ocurrió
	 */
	public void publishAlert(String incidentType, String roadSegment, int position) {
		if (this.client == null) {
			this._debug("Client not initialized, skipping alert publication");
			return;
		}
		
		try {
			// Construir mensaje de alerta con formato correcto del simulador
			// Formato: { "msg": { "event": "OPEN", "rt": "accidente", "id": ..., "vehicle": ..., "road-segment": ..., "position": ... }, "id": "MSG_...", "type": "ACCIDENT", "timestamp": ... }
			JSONObject msgContent = new JSONObject();
			
			// Determinar el tipo de evento (OPEN para abrir, CLOSE para cerrar)
			// Por defecto, cuando se notifica un incidente, se abre (OPEN)
			String eventType = "OPEN";
			String rt = incidentType; // rt puede ser "accidente", "avería", etc.
			
			// Generar ID único para el accidente
			String accidentId = "ACC_" + this.smartcar.getSmartCarID() + "_" + System.currentTimeMillis();
			
			msgContent.put("event", eventType);
			msgContent.put("rt", rt);
			msgContent.put("id", accidentId);
			msgContent.put("vehicle", this.smartcar.getSmartCarID());
			msgContent.put("road-segment", roadSegment);
			msgContent.put("position", position);
			
			JSONObject alertMessage = new JSONObject();
			alertMessage.put("msg", msgContent);
			alertMessage.put("id", "MSG_" + System.currentTimeMillis());
			alertMessage.put("type", "ACCIDENT");
			alertMessage.put("timestamp", System.currentTimeMillis());
			
			// Topic del canal de alertas en AWS IoT
			// Formato: smartcities/traffic/PTPaterna/road/{road-segment}/alerts
			String topic = "smartcities/traffic/PTPaterna/road/" + roadSegment + "/alerts";
			
			// Publicar
			AWSIotMessage message = new AWSIotMessage(topic, QOS, alertMessage.toString());
			this.client.publish(message);
			
			this._debug("Published alert to AWS IoT topic [" + topic + "]: " + alertMessage.toString());
			
		} catch (AWSIotException e) {
			System.err.println("Error publishing alert to AWS IoT: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Error building alert message: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void _debug(String message) {
		System.out.println("(AWSAlertsPublisher: " + this.smartcar.getSmartCarID() + ") " + message);
	}
	
	public boolean isConnected() {
		return this.client != null;
	}
}

