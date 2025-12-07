package dispositivo.interfaces;

public interface Configuracion {

	public static final String M2MIO_USERNAME = "<m2m.io username>";
	public static final String M2MIO_PASSWORD_MD5 = "<m2m.io password (MD5 sum of password)>";
	
//	public static final String TOPIC_BASE = "es/upv/inf/muiinf/ina/";
	public static final String TOPIC_BASE = "ina_08/"; // Ejercicio 6 : La propiedad TOPIC_BASE sirve 
											    // como prefijo configurable para todos los topics MQTT utilizados por el dispositivo IoT. 
												// Es un mecanismo de namespacing que permite organizar y categorizar los mensajes MQTT en una infraestructura IoT.

	public static final String TOPIC_REGISTRO =  Configuracion.TOPIC_BASE + "gestion/dispositivos";


}
