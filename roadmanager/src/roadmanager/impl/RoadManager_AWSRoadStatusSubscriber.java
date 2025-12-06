package roadmanager.impl;

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
 * Suscriptor a mensajes ROAD_STATUS del canal info en AWS IoT
 * Procesa el estado de densidad de tráfico para auto-regulación de velocidad
 */
public class RoadManager_AWSRoadStatusSubscriber extends AWSIotTopic {
	
	private RoadManager roadManager;
	private AWSIotMqttClient client = null;
	
	// Configuración AWS IoT
	private static final String CLIENT_ENDPOINT = "a1knxlrh9s811y-ats.iot.us-east-1.amazonaws.com";
	private static final String CERTS_DIR = "./certs/";
	private static final String THING_NAME = "RoadManager097"; // Nombre del Thing en AWS IoT
	private static final String CERTIFICATE_FILE_DEFAULT = CERTS_DIR + "dispositivo2-certificate.pem.crt";
	private static final String PRIVATE_KEY_FILE_DEFAULT = CERTS_DIR + "dispositivo2-private.pem.key";
	
	private static final AWSIotQos QOS = AWSIotQos.QOS0;
	
	/**
	 * Constructor para el handler del topic info
	 */
	public RoadManager_AWSRoadStatusSubscriber(RoadManager roadManager, String topic) {
		super(topic, QOS);
		this.roadManager = roadManager;
	}
	
	/**
	 * Conecta al cliente AWS IoT MQTT y se suscribe al topic info
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
			String clientId = "road-manager-" + this.roadManager.getManagerId() + "-status-sub-" + UUID.randomUUID().toString();
			this.client = new AWSIotMqttClient(CLIENT_ENDPOINT, clientId, pair.keyStore, pair.keyPassword);
			
			// Conectar
			this.client.connect();
			this._debug("Connected to AWS IoT: " + CLIENT_ENDPOINT);
			
			// Suscribirse al topic (este handler ya es un AWSIotTopic)
			this.client.subscribe(this);
			this._debug("Subscribed to info topic: " + this.getTopic());
			
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
	 * Suscribe a un topic (puede usar wildcards)
	 * Nota: Este método no se usa directamente, la suscripción se hace en connect()
	 * Se mantiene por compatibilidad pero el topic se pasa en el constructor
	 */
	public void subscribe(String topic) {
		if (this.client == null) {
			return;
		}
		
		try {
			// Suscribirse usando este handler (que ya es un AWSIotTopic)
			this.client.subscribe(this);
			this._debug("Subscribed to info topic: " + this.getTopic());
			
		} catch (AWSIotException e) {
			System.err.println("Error subscribing to info topic: " + e.getMessage());
		}
	}
	
	/**
	 * Método llamado automáticamente cuando se recibe un mensaje en el topic suscrito
	 */
	@Override
	public void onMessage(AWSIotMessage message) {
		String payload = message.getStringPayload();
		
		try {
			// Extraer el road-segment del topic
			String topic = message.getTopic();
			String[] topicParts = topic.split("/");
			String roadSegment = topicParts[topicParts.length - 2]; // Antes de "info"
			
			// Parsear el mensaje JSON
			JSONObject jsonMessage = new JSONObject(payload);
			JSONObject msgContent = null;
			String messageType = null;
			
			// Detectar formato: puede ser con wrapper o directo
			if (jsonMessage.has("msg")) {
				msgContent = jsonMessage.getJSONObject("msg");
				messageType = jsonMessage.optString("type", null);
			} else {
				msgContent = jsonMessage;
				messageType = jsonMessage.optString("type", null);
			}
			
			// Procesar solo mensajes ROAD_STATUS
			if ("ROAD_STATUS".equals(messageType) && msgContent != null) {
				String status = msgContent.optString("status", "UNKNOWN");
				int maxSpeed = msgContent.optInt("max-speed", msgContent.optInt("current-max-speed", 60));
				
				this._debug("ROAD_STATUS received for " + roadSegment + " - Status: " + status + ", Max Speed: " + maxSpeed);
				
				// Procesar el estado para auto-regulación de velocidad
				this.roadManager.processRoadStatus(roadSegment, status, maxSpeed);
			}
			
		} catch (Exception e) {
			System.err.println("Error processing road status: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void _debug(String message) {
		System.out.println("(RoadManager AWSRoadStatusSubscriber: " + this.roadManager.getManagerId() + ") " + message);
	}
	
	public boolean isConnected() {
		return this.client != null;
	}
}

