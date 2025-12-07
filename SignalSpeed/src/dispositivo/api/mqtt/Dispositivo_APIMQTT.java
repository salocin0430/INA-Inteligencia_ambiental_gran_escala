package dispositivo.api.mqtt;

import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONException;
import org.json.JSONObject;

import dispositivo.interfaces.Configuracion;
import dispositivo.interfaces.IDispositivo;
import dispositivo.interfaces.IFuncion;
import dispositivo.utils.MySimpleLogger;

public class Dispositivo_APIMQTT implements MqttCallback {

	protected MqttClient myClient;
	protected MqttConnectOptions connOpt;
	protected String clientId = null;
	
	protected IDispositivo dispositivo;
	protected String mqttBroker = null;
	
	private String loggerId = null;
	
	public static Dispositivo_APIMQTT build(IDispositivo dispositivo, String brokerURL) {
		Dispositivo_APIMQTT api = new Dispositivo_APIMQTT(dispositivo);
		api.setBroker(brokerURL);
		return api;
	}
	
	protected Dispositivo_APIMQTT(IDispositivo dev) {
		this.dispositivo = dev;
		this.loggerId = dev.getId() + "-apiMQTT";
	}
	
	protected void setBroker(String mqttBrokerURL) {
		this.mqttBroker = mqttBrokerURL;
	}
	
	
	@Override
	public void connectionLost(Throwable t) {
		MySimpleLogger.debug(this.loggerId, "Connection lost!");
		// code to reconnect to the broker would go here if desired
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		//System.out.println("Pub complete" + new String(token.getMessage().getPayload()));
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		
		String payload = new String(message.getPayload());
		
		System.out.println("-------------------------------------------------");
		System.out.println("| Topic:" + topic);
		System.out.println("| Message: " + payload);
		System.out.println("-------------------------------------------------");
		
		// DO SOME MAGIC HERE!
		//

		// Obtenemos el id de la función

		//   Los topics están organizados de la siguiente manera:

		//         $topic_base/dispositivo/funcion/$ID-FUNCION/commamnd

		//   Donde el $topic_base es parametrizable al arrancar el dispositivo

		//   y la $ID-FUNCION es el identificador de la dunción
		//
		// Ejercicio 7 y 8: Procesar mensajes en formato JSON
		//

		// Ejecutamos acción indicada en campo 'accion' del JSON recibido
		String action = null;
		try {
			JSONObject payloadJSON = new JSONObject(payload);
			action = payloadJSON.getString("accion");
		} catch (JSONException e) {
			MySimpleLogger.warn(this.loggerId, "Error al parsear JSON: " + payload + ". Formato esperado: {\"accion\":\"encender\"}");
			return;
		}
		
		// Distinguir entre comandos de dispositivo y comandos de función
		String[] topicNiveles = topic.split("/");
		
		// Verificar si es un comando de función: dispositivo/{id}/funcion/{funcion}/comandos
		if (topicNiveles.length >= 4 && 
			topicNiveles[topicNiveles.length-3].equals("funcion") && 
			topicNiveles[topicNiveles.length-1].equals("comandos")) {
			
			// Es un comando de función
			String funcionId = topicNiveles[topicNiveles.length-2];
			
			IFuncion f = this.dispositivo.getFuncion(funcionId);
			if (f == null) {
				MySimpleLogger.warn(this.loggerId, "No encontrada funcion " + funcionId);
				return;
			}
			
			if (action.equalsIgnoreCase("encender"))
				f.encender();
			else if (action.equalsIgnoreCase("apagar"))
				f.apagar();
			else if (action.equalsIgnoreCase("parpadear"))
				f.parpadear();
			else
				MySimpleLogger.warn(this.loggerId, "Acción de función '" + action + "' no reconocida. Sólo admitidas: encender, apagar o parpadear");
				
		} else if (topicNiveles.length >= 3 && 
				   topicNiveles[topicNiveles.length-1].equals("comandos") &&
				   !topicNiveles[topicNiveles.length-2].equals("funcion")) {
			
			// Es un comando de dispositivo: dispositivo/{id}/comandos
			if (action.equalsIgnoreCase("habilitar")) {
				this.dispositivo.habilitar();
				MySimpleLogger.info(this.loggerId, "Dispositivo habilitado via MQTT");
			} else if (action.equalsIgnoreCase("deshabilitar")) {
				this.dispositivo.deshabilitar();
				MySimpleLogger.info(this.loggerId, "Dispositivo deshabilitado via MQTT");
			} else {
				MySimpleLogger.warn(this.loggerId, "Acción de dispositivo '" + action + "' no reconocida. Sólo admitidas: habilitar, deshabilitar");
			}
		} else {
			MySimpleLogger.warn(this.loggerId, "Topic no reconocido: " + topic);
		}

		
		
	}

	/**
	 * 
	 * runClient
	 * The main functionality of this simple example.
	 * Create a MQTT client, connect to broker, pub/sub, disconnect.
	 * 
	 */
	public void connect() {
		// setup MQTT Client
		String clientID = this.dispositivo.getId() + UUID.randomUUID().toString() + ".subscriber";
		connOpt = new MqttConnectOptions();
		
		connOpt.setCleanSession(true);
		connOpt.setKeepAliveInterval(30);
//			connOpt.setUserName(M2MIO_USERNAME);
//			connOpt.setPassword(M2MIO_PASSWORD_MD5.toCharArray());
		
		// Connect to Broker
		try {
			MqttDefaultFilePersistence persistence = null;
			try {
				persistence = new MqttDefaultFilePersistence("/tmp");
			} catch (Exception e) {
			}
			if ( persistence != null )
				myClient = new MqttClient(this.mqttBroker, clientID, persistence);
			else
				myClient = new MqttClient(this.mqttBroker, clientID);

			myClient.setCallback(this);
			myClient.connect(connOpt);
		} catch (MqttException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		MySimpleLogger.info(this.loggerId, "Conectado al broker " + this.mqttBroker);

	}
	
	
	public void disconnect() {
		
		// disconnect
		try {
			// wait to ensure subscribed messages are delivered
			Thread.sleep(10000);

			myClient.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}

		
	}

	
	protected void subscribe(String myTopic) {
		
		// subscribe to topic
		try {
			int subQoS = 0;
			myClient.subscribe(myTopic, subQoS);
			MySimpleLogger.info(this.loggerId, "Suscrito al topic " + myTopic);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	

	protected void unsubscribe(String myTopic) {
		
		// unsubscribe to topic
		try {
			myClient.unsubscribe(myTopic);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void iniciar() {

		if ( this.myClient == null || !this.myClient.isConnected() )
			this.connect();
		
		if ( this.dispositivo == null )
			return;
		
		// Suscribirse a comandos de funciones individuales
		for(IFuncion f : this.dispositivo.getFunciones())
			this.subscribe(this.calculateCommandTopic(f));
		
		// Ejercicio 8: Suscribirse a comandos del dispositivo
		this.subscribe(this.calculateDeviceCommandTopic());

	}
	
	
	
	public void detener() {
		
		
		// To-Do
		
	}
	
	
	protected String calculateCommandTopic(IFuncion f) {
		return Configuracion.TOPIC_BASE + "dispositivo/" + dispositivo.getId() + "/funcion/" + f.getId() + "/comandos";
	}
	
	protected String calculateInfoTopic(IFuncion f) {
		return Configuracion.TOPIC_BASE + "dispositivo/" + dispositivo.getId() + "/funcion/" + f.getId() + "/info";
	}
	
	// Ejercicio 8: Topic para comandos del dispositivo
	protected String calculateDeviceCommandTopic() {
		return Configuracion.TOPIC_BASE + "dispositivo/" + dispositivo.getId() + "/comandos";
	}
	
	// Ejercicio 9: Publisher para notificaciones push
	private FuncionPublisher_APIMQTT funcionPublisher = null;
	
	/**
	 * Publica el estado de una función usando FuncionPublisher_APIMQTT
	 */
	public void publishFunctionStatus(IFuncion funcion) {
		if (funcionPublisher == null) {
			funcionPublisher = FuncionPublisher_APIMQTT.build(this.myClient, this.dispositivo.getId());
		}
		funcionPublisher.publishStatus(funcion);
	}
	
	/**
	 * Obtiene el cliente MQTT para uso externo
	 */
	public MqttClient getMqttClient() {
		return this.myClient;
	}
	

}
