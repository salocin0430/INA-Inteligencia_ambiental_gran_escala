package dispositivo.iniciador;

import dispositivo.componentes.SignalSpeed;
import dispositivo.utils.MySimpleLogger;

/**
 * Ejemplo de uso de SignalSpeed con AWS IoT Device Shadow habilitado.
 * 
 * Este ejemplo muestra cómo:
 * 1. Crear una señal de velocidad (SignalSpeed)
 * 2. Habilitar AWS IoT Device Shadow
 * 3. La señal publicará su estado automáticamente en AWS
 * 4. La señal recibirá comandos remotos desde AWS para activar/desactivar
 * 
 * IMPORTANTE: Antes de ejecutar, asegúrate de:
 * 1. Tener los certificados en certs/ (dispositivo2-certificate.pem.crt, dispositivo2-private.pem.key)
 * 2. Haber creado el Thing en AWS IoT Console
 * 3. Haber asociado el certificado al Thing
 * 4. El Thing debe tener permisos suficientes (ver policies/)
 */
public class SignalSpeedIniciador_AWSShadow {

	public static void main(String[] args) {
		
		// Configuración de la señal de velocidad
		String mqttBroker = "tcp://tambori.dsic.upv.es:1883";  // MQTT LOCAL
		String roadSegment = "R1s1";
		String signalId = "SL_R1s1_001";
		int velocidadMaxima = 50;  // 50 km/h
		int posicionInicio = 0;
		int posicionFin = 580;
		
		// Nombre del Thing en AWS IoT (debe existir en AWS IoT Console)
		String awsThingName = "SignalSpeed_SL_R1s1_001";  // Cambia esto por tu Thing name en AWS
		
		MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "==============================================");
		MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "SignalSpeed con AWS IoT Device Shadow");
		MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "==============================================");
		
		try {
			// Crear la señal de velocidad
			SignalSpeed signal = new SignalSpeed(
				roadSegment, 
				signalId, 
				velocidadMaxima, 
				posicionInicio, 
				posicionFin, 
				mqttBroker
			);
			
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "Señal creada: " + signalId);
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "  Road Segment: " + roadSegment);
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "  Velocidad Máxima: " + velocidadMaxima + " km/h");
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "  Posición: " + posicionInicio + "-" + posicionFin + " m");
			
			// Habilitar AWS IoT Device Shadow
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "Habilitando AWS IoT Device Shadow...");
			signal.enableAWSShadow(awsThingName);
			
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "==============================================");
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "AWS IoT Device Shadow habilitado");
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "  Thing Name: " + awsThingName);
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "==============================================");
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "");
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "Estado:");
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "  - La señal publicará su estado en AWS");
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "  - Se puede activar/desactivar remotamente desde AWS IoT Console");
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "  - Los eventos de step se reciben del topic local MQTT");
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "");
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "Presiona Ctrl+C para detener");
			MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "==============================================");
			
			// Publicar estado inicial
			signal.publicarEstado();
			
			// Mantener el programa corriendo indefinidamente
			// La señal está suscrita al topic 'step' y se activará en cada paso de simulación
			try {
				while (true) {
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "Programa interrumpido");
			} finally {
				// Cerrar todas las conexiones
				signal.cerrar();
				MySimpleLogger.info("SignalSpeedIniciador_AWSShadow", "Conexiones cerradas correctamente");
			}
			
		} catch (Exception e) {
			MySimpleLogger.error("SignalSpeedIniciador_AWSShadow", "Error al iniciar SignalSpeed con AWS: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
	}
}
