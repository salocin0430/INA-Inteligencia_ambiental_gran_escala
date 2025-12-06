package smartcar.starter;

import smartcar.impl.SmartCar;
import ina.vehicle.navigation.components.Route;

/**
 * Ejemplo de uso de SmartCar con AWS IoT Device Shadow habilitado.
 * 
 * Este ejemplo muestra cómo:
 * 1. Crear un SmartCar
 * 2. Asignar una ruta
 * 3. Habilitar AWS IoT Device Shadow
 * 4. El vehículo publicará su estado automáticamente en cada paso de simulación
 * 5. El vehículo recibirá comandos para cambiar la velocidad de crucero
 * 
 * IMPORTANTE: Antes de ejecutar, asegúrate de:
 * 1. Tener los certificados en certs/ (ver AWS_SHADOW_CONFIG.md)
 * 2. Haber creado el Thing en AWS IoT Console
 * 3. Haber asociado el certificado al Thing
 */
public class SmartCarStarter_AWSShadow {

	public static void main(String[] args) {
		
		// Crear un SmartCar
		SmartCar car = new SmartCar("SmartCar001");
		car.setCruiserSpeed(60);
		
		// Crear una ruta de ejemplo
		Route ruta = new Route();
		// Ruta: R5s1 (0-580) → R1s4a (490-600)
		ruta.addRouteFragment("R5s1", 0, 580);
		ruta.addRouteFragment("R1s4a", 490, 600);
		
		// Asignar ruta al vehículo
		car.setRoute(ruta);
		
		// Habilitar AWS IoT Device Shadow
		// IMPORTANTE: El nombre del Thing debe existir en AWS IoT Console
		String thingName = "SmartCar097"; // Cambia esto por el nombre de tu Thing
		car.enableAWSShadow(thingName);
		
		System.out.println("==========================================");
		System.out.println("SmartCar con AWS Shadow iniciado");
		System.out.println("Thing Name: " + thingName);
		System.out.println("==========================================");
		System.out.println("El vehículo publicará su estado en cada paso de simulación");
		System.out.println("Puedes cambiar la velocidad de crucero desde AWS IoT Console");
		System.out.println("Presiona Ctrl+C para detener");
		System.out.println("==========================================");
		
		// Mantener el programa corriendo
		// El vehículo se moverá automáticamente en cada paso de simulación
		// y publicará su estado en AWS Shadow
		try {
			// Mantener corriendo indefinidamente
			while (true) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			System.out.println("Programa interrumpido");
		} finally {
			// Desconectar todos los componentes
			car.disconnect();
			System.out.println("SmartCar desconectado");
		}
	}
}

