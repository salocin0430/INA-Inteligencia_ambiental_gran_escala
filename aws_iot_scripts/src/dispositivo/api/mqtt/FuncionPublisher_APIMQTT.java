package dispositivo.api.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.json.JSONException;
import org.json.JSONObject;

import dispositivo.interfaces.Configuracion;
import dispositivo.interfaces.IFuncion;
import dispositivo.utils.MySimpleLogger;

/**
 * Ejercicio 9: Componente para publicar notificaciones push sobre funciones
 * Publica el estado de las funciones en topics MQTT cuando cambian
 */
public class FuncionPublisher_APIMQTT {
	
	protected MqttClient mqttClient;
	protected String dispositivoId;
	protected String loggerId;
	
	public static FuncionPublisher_APIMQTT build(MqttClient mqttClient, String dispositivoId) {
		FuncionPublisher_APIMQTT publisher = new FuncionPublisher_APIMQTT();
		publisher.mqttClient = mqttClient;
		publisher.dispositivoId = dispositivoId;
		publisher.loggerId = dispositivoId + "-FuncionPublisher";
		return publisher;
	}
	
	protected FuncionPublisher_APIMQTT() {
	}
	
	/**
	 * Publica el estado de una función en su topic de info
	 * @param funcion La función cuyo estado se va a publicar
	 */
	public void publishStatus(IFuncion funcion) {
		if (mqttClient == null || !mqttClient.isConnected()) {
			MySimpleLogger.warn(loggerId, "Cliente MQTT no conectado, no se puede publicar estado de " + funcion.getId());
			return;
		}
		
		try {
			// Crear el mensaje JSON con el estado de la función
			JSONObject statusMessage = new JSONObject();
			statusMessage.put("id", funcion.getId());
			statusMessage.put("estado", funcion.getStatus().name());
			
			// Calcular el topic de info para esta función
			String infoTopic = calculateInfoTopic(funcion);
			
			// Crear el mensaje MQTT
			MqttMessage message = new MqttMessage(statusMessage.toString().getBytes());
			message.setQos(0);
			message.setRetained(false);
			
			// Publicar el mensaje
			MqttTopic topic = mqttClient.getTopic(infoTopic);
			topic.publish(message);
			
			MySimpleLogger.debug(loggerId, "Publicado estado de " + funcion.getId() + " en topic " + infoTopic + ": " + statusMessage.toString());
			
		} catch (JSONException e) {
			MySimpleLogger.error(loggerId, "Error al crear JSON para función " + funcion.getId() + ": " + e.getMessage());
		} catch (MqttException e) {
			MySimpleLogger.error(loggerId, "Error al publicar estado de función " + funcion.getId() + ": " + e.getMessage());
		}
	}
	
	/**
	 * Calcula el topic de info para una función específica
	 * @param funcion La función
	 * @return El topic de info
	 */
	protected String calculateInfoTopic(IFuncion funcion) {
		return Configuracion.TOPIC_BASE + "dispositivo/" + dispositivoId + "/funcion/" + funcion.getId() + "/info";
	}
}
