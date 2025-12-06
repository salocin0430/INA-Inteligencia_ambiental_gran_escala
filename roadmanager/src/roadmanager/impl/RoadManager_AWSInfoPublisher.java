package roadmanager.impl;

import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMessage;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotQos;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil;
import com.amazonaws.services.iot.client.sample.sampleUtil.SampleUtil.KeyStorePasswordPair;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Publisher para publicar información en el canal info de los segmentos en AWS IoT
 * Retransmite alertas y publica señales de tráfico
 */
public class RoadManager_AWSInfoPublisher {
	
	private RoadManager roadManager;
	private AWSIotMqttClient client = null;
	
	// Configuración AWS IoT
	private static final String CLIENT_ENDPOINT = "a1knxlrh9s811y-ats.iot.us-east-1.amazonaws.com";
	private static final String CERTS_DIR = "./certs/";
	private static final String THING_NAME = "RoadManager097"; // Nombre del Thing en AWS IoT
	private static final String CERTIFICATE_FILE_DEFAULT = CERTS_DIR + "dispositivo2-certificate.pem.crt";
	private static final String PRIVATE_KEY_FILE_DEFAULT = CERTS_DIR + "dispositivo2-private.pem.key";
	
	private static final AWSIotQos QOS = AWSIotQos.QOS0;
	
	public RoadManager_AWSInfoPublisher(RoadManager roadManager) {
		this.roadManager = roadManager;
	}
	
	/**
	 * Conecta al cliente AWS IoT MQTT
	 */
	public void connect() {
		try {
			// Intentar usar certificado específico del Thing (RoadManager097), si no existe, usar el default
			String certFile = CERTS_DIR + THING_NAME + "-certificate.pem.crt";
			String keyFile = CERTS_DIR + THING_NAME + "-private.pem.key";
			
			java.io.File certFileObj = new java.io.File(certFile);
			java.io.File keyFileObj = new java.io.File(keyFile);
			
			// Si no existe el certificado específico del Thing, usar el default (dispositivo2)
			if (!certFileObj.exists() || !keyFileObj.exists()) {
				certFile = CERTIFICATE_FILE_DEFAULT;
				keyFile = PRIVATE_KEY_FILE_DEFAULT;
				this._debug("Using default certificate (dispositivo2) for Thing: " + THING_NAME);
			} else {
				this._debug("Using specific certificate for Thing: " + THING_NAME);
			}
			
			// Inicializar cliente AWS IoT
			KeyStorePasswordPair pair = SampleUtil.getKeyStorePasswordPair(certFile, keyFile);
			String clientId = "road-manager-" + this.roadManager.getManagerId() + "-info-pub-" + UUID.randomUUID().toString();
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
	 * Publica un mensaje de información (retransmisión de alerta) en el canal info
	 * @param roadSegment Segmento de carretera
	 * @param infoMessage Mensaje JSON a publicar
	 */
	public void publishInfo(String roadSegment, String infoMessage) {
		if (this.client == null) {
			this._debug("Client not initialized, skipping info publication");
			return;
		}
		
		try {
			String topic = "smartcities/traffic/PTPaterna/road/" + roadSegment + "/info";
			AWSIotMessage message = new AWSIotMessage(topic, QOS, infoMessage);
			this.client.publish(message);
			this._debug("Published info to AWS IoT topic [" + topic + "]: " + infoMessage);
			
		} catch (AWSIotException e) {
			System.err.println("Error publishing info to AWS IoT: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Error building info message: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Publica una señal de límite de velocidad
	 * @param roadSegment Segmento de carretera
	 * @param signal Señal de límite de velocidad
	 */
	public void publishSpeedLimitSignal(String roadSegment, SpeedLimitSignal signal) {
		if (this.client == null) {
			return;
		}
		
		try {
			// Construir mensaje de señal speed-limit
			JSONObject signalMessage = new JSONObject();
			signalMessage.put("type", "SPEED_LIMIT");
			signalMessage.put("value", signal.getValue());
			signalMessage.put("position-start", signal.getPositionStart());
			signalMessage.put("position-end", signal.getPositionEnd());
			signalMessage.put("validity", signal.getValidity());
			
			// Publicar en el topic de señales del segmento
			String topic = "smartcities/traffic/PTPaterna/road/" + roadSegment + "/signals";
			AWSIotMessage message = new AWSIotMessage(topic, QOS, signalMessage.toString());
			this.client.publish(message);
			this._debug("Published speed-limit signal to AWS IoT topic [" + topic + "]: " + signalMessage.toString());
			
		} catch (Exception e) {
			System.err.println("Error publishing speed-limit signal: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Elimina la señal de límite de velocidad
	 * @param roadSegment Segmento de carretera
	 */
	public void removeSpeedLimitSignal(String roadSegment) {
		if (this.client == null) {
			return;
		}
		
		try {
			// Publicar señal con validez 0 para indicar eliminación
			JSONObject signalMessage = new JSONObject();
			signalMessage.put("type", "SPEED_LIMIT");
			signalMessage.put("value", 0);
			signalMessage.put("position-start", 0);
			signalMessage.put("position-end", 0);
			signalMessage.put("validity", 0);
			
			String topic = "smartcities/traffic/PTPaterna/road/" + roadSegment + "/signals";
			AWSIotMessage message = new AWSIotMessage(topic, QOS, signalMessage.toString());
			this.client.publish(message);
			this._debug("Removed speed-limit signal from AWS IoT topic [" + topic + "]");
			
		} catch (Exception e) {
			System.err.println("Error removing speed-limit signal: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void _debug(String message) {
		System.out.println("(RoadManager AWSInfoPublisher: " + this.roadManager.getManagerId() + ") " + message);
	}
	
	public boolean isConnected() {
		return this.client != null;
	}
}

