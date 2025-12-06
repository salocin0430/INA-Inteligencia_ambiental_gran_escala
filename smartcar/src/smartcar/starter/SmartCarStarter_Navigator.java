package smartcar.starter;

import smartcar.impl.SmartCar;
import ina.vehicle.navigation.components.Route;
import ina.vehicle.navigation.interfaces.IRoute;

/**
 * Ejemplo de uso del SmartCar con Navigator integrado.
 * 
 * Este ejemplo muestra cómo:
 * 1. Crear un SmartCar
 * 2. Asignarle una ruta usando Navigator
 * 3. El vehículo se moverá automáticamente en cada paso de simulación
 */
public class SmartCarStarter_Navigator {

	public static void main(String[] args) {

		// Crear un SmartCar
		SmartCar sc1 = new SmartCar("SmartCar001");
		sc1.setVehicleRole("PrivateUsage");
		sc1.setCruiserSpeed(60); // 60 km/h
		
		// Crear una ruta usando Navigator
		IRoute ruta = new Route();
		
		// OPCIÓN 1: Ruta simple - Solo R5s1 hasta el final (0 a 580)
		// Del JSON: R5s1 tiene start-kp=0, end-kp=580, length=580
		//ruta.addRouteFragment("R5s1", 0, 580);
		
		// OPCIÓN 2: Ruta con cambio de segmento - R5s1 → R1s4a (según el mapa)
		// Según el mapa visual: R5s1 termina en 580 y se conecta con R1s4a en punto 490
		ruta.addRouteFragment("R5s1", 0, 580);      // Todo R5s1 hasta el final
		ruta.addRouteFragment("R1s4a", 490, 600);   // Continuar en R1s4a desde punto 490
		
		// OPCIÓN 3: Ruta del Proyecto - De R5s1 punto 100 a R1s4a punto 600
		// ruta.addRouteFragment("R5s1", 100, 580);    // Desde punto 100 hasta el final de R5s1
		// ruta.addRouteFragment("R1s4a", 490, 600);  // Continuar en R1s4a desde 490 hasta 600
		
		// OPCIÓN 4: Ruta con cambio de segmento REAL (R2 - segmentos conectados)
		// Del JSON: R2s1 termina en 300, R2s2 empieza en 300 (están conectados)
		// ruta.addRouteFragment("R2s1", 0, 300);      // R2s1 completo
		// ruta.addRouteFragment("R2s2", 300, 750);    // Continuar en R2s2 (conectado en punto 300)
		
		// Asignar la ruta al SmartCar
		// Esto crea el Navigator internamente y lo inicia
		sc1.setRoute(ruta);
		
		System.out.println("SmartCar creado con ruta:");
		System.out.println("  - ID: " + sc1.getSmartCarID());
		System.out.println("  - Role: " + sc1.getVehicleRole());
		System.out.println("  - Velocidad crucero: " + sc1.getCruiserSpeed() + " km/h");
		System.out.println("  - Ruta: " + ruta);
		System.out.println("\nEl vehículo se moverá automáticamente en cada paso de simulación.");
		System.out.println("Suscrito al topic: es/upv/pros/tatami/smartcities/traffic/PTPaterna/step");
		System.out.println("\nNOTA: Cuando llegue al final de la ruta, el Navigator se detendrá.");
		System.out.println("Presiona Ctrl+Cx para detener...");
		
		// Mantener el programa corriendo para recibir mensajes MQTT
		try {
			while (true) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			System.out.println("\nDeteniendo SmartCar...");
			sc1.disconnect();
		}
	}
}

