package smartcar.impl;

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
 * Subscriber para AWS IoT Device Shadow.
 * Escucha cambios en el estado "desired" del Device Shadow para recibir comandos.
 * 
 * Topics:
 * - $aws/things/{thingName}/shadow/update/delta - Recibe solo los cambios (delta)
 * - $aws/things/{thingName}/shadow/update/accepted - Confirmación de actualización
 */
public class SmartCar_AWSShadowSubscriber extends AWSIotTopic {

	protected SmartCar smartcar = null;
	protected String thingName = null;
	protected AWSIotMqttClient client = null;
	
	// Configuración AWS IoT
	private static final String CLIENT_ENDPOINT = "a1knxlrh9s811y-ats.iot.us-east-1.amazonaws.com";
	private static final String CERTS_DIR = "./certs/";
	
	// Certificados: puedes usar dispositivo2 (para múltiples Things) o certificados específicos
	// Opción 1: Usar certificado compartido (dispositivo2) - funciona para cualquier Thing
	private static final String CERTIFICATE_FILE_DEFAULT = CERTS_DIR + "dispositivo2-certificate.pem.crt";
	private static final String PRIVATE_KEY_FILE_DEFAULT = CERTS_DIR + "dispositivo2-private.pem.key";
	
	private static final AWSIotQos QOS = AWSIotQos.QOS0;
	
	/**
	 * Constructor para el handler del topic delta
	 */
	public SmartCar_AWSShadowSubscriber(SmartCar smartcar, String thingName, String topic) {
		super(topic, QOS);
		this.smartcar = smartcar;
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
			String clientId = "smartcar-sub-" + this.smartcar.getSmartCarID() + "-" + UUID.randomUUID().toString();
			this.client = new AWSIotMqttClient(CLIENT_ENDPOINT, clientId, pair.keyStore, pair.keyPassword);
			
			// Conectar
			this.client.connect();
			this._debug("Connected to AWS IoT: " + CLIENT_ENDPOINT);
			
			// Suscribirse al topic delta (usar este mismo objeto como handler)
			String deltaTopic = "$aws/things/" + this.thingName + "/shadow/update/delta";
			// Actualizar el topic de este handler
			// Nota: AWSIotTopic no permite cambiar el topic después de la construcción,
			// así que creamos un nuevo handler con el topic correcto
			SmartCar_AWSShadowSubscriber deltaHandler = new SmartCar_AWSShadowSubscriber(this.smartcar, this.thingName, deltaTopic);
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
			System.err.println("Error disconnecting from AWS IoT: " + e.getMessage());
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
			
			// El mensaje delta tiene formato: { "state": { "cruiser-speed": 80, ... }, "version": 123, ... }
			JSONObject state = jsonMessage.optJSONObject("state");
			if (state == null) {
				this._debug("No 'state' field in delta message");
				return;
			}
			
			// Procesar cambios en el estado desired
			processDesiredState(state);
			
		} catch (Exception e) {
			System.err.println("Error processing shadow delta message: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Procesa los cambios en el estado "desired" y aplica los comandos al SmartCar.
	 * Comandos soportados:
	 * - cruiser-speed: Cambia la velocidad de crucero del vehículo
	 */
	private void processDesiredState(JSONObject desiredState) {
		try {
			// Comando: cambiar velocidad de crucero
			if (desiredState.has("cruiser-speed")) {
				int newCruiserSpeed = desiredState.getInt("cruiser-speed");
				this._debug("Received command to change cruiser-speed to: " + newCruiserSpeed + " km/h");
				
				// Validar rango (0-200 km/h)
				if (newCruiserSpeed >= 0 && newCruiserSpeed <= 200) {
					this.smartcar.setCruiserSpeed(newCruiserSpeed);
					this._debug("Cruiser speed updated to: " + newCruiserSpeed + " km/h");
				} else {
					this._debug("Invalid cruiser-speed value: " + newCruiserSpeed + " (must be 0-200)");
				}
			}
			
			// Aquí se pueden agregar más comandos en el futuro:
			// - Cambiar ruta
			// - Activar/desactivar modo especial
			// - etc.
			
		} catch (Exception e) {
			System.err.println("Error processing desired state: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void _debug(String message) {
		System.out.println("(AWSShadowSubscriber: " + this.smartcar.getSmartCarID() + ") " + message);
	}
	
	public boolean isConnected() {
		// AWSIotMqttClient no tiene método isConnected()
		// Simplemente verificamos que el cliente esté inicializado
		// La conexión se maneja con excepciones al suscribirse
		return this.client != null;
	}
}

